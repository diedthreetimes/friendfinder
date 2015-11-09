require 'hbc_psi'
require 'hbc_bearer_psi_ca'
require 'ca'
require 'open-uri'
require 'pry'

class AuthorityController < ApplicationController
  respond_to :json, :html

  def login
    #TODO: Store the redirected url in the session and then use it again here

#    request_token = oclient.request_token(:oauth_callback => "http://#{request.host_with_port}/authority/download_connections.#{request.format.to_sym}")
    request_token = oclient.get_request_token(:oauth_callback => "http://#{request.host_with_port}/authority/download_connections.#{request.format.to_sym}")
    session[:rtoken] = request_token.token
    session[:rsecret] = request_token.secret
    session[:rtoken] = request_token

    #puts request.format.to_sym

    redirect_to request_token.authorize_url(:oauth_callback => "http://#{request.host_with_port}/authority/download_connections.#{request.format.to_sym}")
    # TODO: Do we want to set a cookie here? We should probably just use an CookieStore. If so we should stirctly enforce an ssl connection. Or, we could use an encrypted cookiestore of some kind.
  end

  def index
    respond_with( {success:"True"} )
  end

  def logout
    session[:atoken] = nil
    redirect_to action: 'index'
  end

  # For later this will get all screen names/profile_images without an extra download
  # client.following.attrs[:users].collect{|v| v[:screen_name]}

  def download_profile
    with_log_in do |c|
      respond_with( CA.sign(get_profile(c).to_json, false).to_pem )
    end
  end

  def test_identity
    with_log_in do |c|
      resp = {}

      resp[:profile] = get_profile(c)
      resp[:profile][:avatar] =  {:url => get_profile_image_url(c)}
      #resp[:profile][:site_standard_profile_request] = resp[:profile][:site_standard_profile_request][:url]
      resp[:profile][:site_standard_profile_request] = get_profile(c)[:profile_url]

      hash = Digest::SHA1.new
      open( resp[:profile][:avatar][:url] ) { |f|
        hash << f.read
      }

      resp[:profile][:avatar][:hash] = hash.base64digest

      resp[:sig] = CA.sign( resp.as_json.to_s ).to_pem

      respond_with( resp )
    end
  end

  def test_connections
    resp = {}

    num_random_friend = 450
    random_friend = rand(1...num_random_friend)
    friends = 50.times.to_a + num_random_friend.times.to_a.collect{ |n| n+num_random_friend*random_friend}
    resp[:random_friend] = random_friend

    resp[:count] = friends.count
    if !params[:include_connections].nil?
      resp[:connections] = friends.collect{|x| x.to_s }
    end

    # resp[:psi_message] = HbcPsi.sig_message(friends.collect{|x| x.to_s})
    if params[:protocol] == 'b_psi_ca'
      resp[:psi_message] = HbcBPsiCa.issue_capabilities(friends.collect {|x| x})
    else
      resp[:psi_message] = HbcPsi.sig_message(friends.collect{|x| x.to_s}, params[:protocol])
    end

    respond_with( resp )
  end

  def download_connections
    with_log_in do |c|
#      connections = c.connections[:all]
      friends = c.followers
      private_friends = []

#      private_friends = connections.select{|f| f.first_name == "private"}
#      friends = connections.select{|f| f.first_name != "private"}

      resp = {}
      resp[:count] = friends.count
      if !params[:include_connections].nil?
        resp[:connections] = friends.collect {|v| v.id}
        resp[:privates] = private_friends.count
      end
      #resp[:connections] = c.profile( id: friends.first["id"], fields: %w(positions) )

      if params[:protocol] == 'b_psi_ca'
        resp[:psi_message] = HbcBPsiCa.issue_capabilities(friends.collect {|v| v[:id]})
      else
        resp[:psi_message] = HbcPsi.sig_message(friends.collect {|v| v[:id]}, params[:protocol])
      end

      envelope = {}
      envelope[:profile] = get_profile(c)
      envelope[:profile][:avatar] =  {:url => get_profile_image_url(c)}
      envelope[:profile][:site_standard_profile_request] = get_profile(c)[:profile_url]

      hash = Digest::SHA1.new
      open( envelope[:profile][:avatar][:url] ) { |f|
        hash << f.read
      }

      envelope[:profile][:avatar][:hash] = hash.base64digest

      resp[:signed_identity] = CA.sign( envelope.as_json.to_s ).to_pem

      respond_with( resp )
    end
  end

  # TODO: Give a way to download certificate securely. We would need a non self signed certificate to really be secure.
  # TODO: ensure HTTPS only

  private
  def oclient
    #oclient = client
    oclient = OAuth::Consumer.new(
      "tZS0BDKy1c1ruUzSLAz4xNHU2",
      "mSFtnEqUMoBSybjUh5w7HlPgZ7OW3rfzFdUkJwgZdI0zFXkEpM",
      :site => 'https://api.twitter.com' )
  end

  def client
    # @client ||= LinkedIn::Client.new("78imlmkvuvhi", "BbIEylD3TBcAvVeR")
    @client ||= Twitter::REST::Client.new do |config|
      config.consumer_key        = "tZS0BDKy1c1ruUzSLAz4xNHU2"
      config.consumer_secret     = "mSFtnEqUMoBSybjUh5w7HlPgZ7OW3rfzFdUkJwgZdI0zFXkEpM"

      atoken = session[:atoken]
      if (atoken.nil?)
        puts "Atoekn nil! Using default token"
        atoken = "315536436-IphHd7qvowKG8hS4X8pM4ZbsOvKPwAoYIPuiFaw2"
      end

      asecret = session[:asecret]
      if (asecret.nil?)
        puts "Asecret nil! Using default token"
        asecret = "6n8mISg0Hd9XwzIEoi75daQJXo1pm4dDhO9ClNd660zF1"
      end

      config.access_token        = atoken
      config.access_token_secret = asecret
    end
  end

  # TODO: Move this (and subsequent similar methods) into a class
  def get_profile c
    @profile ||= {
      :profile_image_url => c.user.profile_image_url.to_s,
      :profile_url => "http://twitter.com/#{c.user.screen_name}"
    }
  end

  def get_profile_image_url c
    # c.picture_urls(:id => c.profile.id)["all"].first
    get_profile(c)[:profile_image_url]
  end


  def with_log_in &block
    if session[:atoken].nil? && params[:oauth_access_token].nil?
      if params[:oauth_verifier].nil?
        redirect_to action: 'login', format: request.format.to_sym
        return
      end

#      pin = params[:oauth_verifier]
#      atoken, asecret = oclient.authorize_from_request(session[:rtoken], session[:rsecret], pin)
      access_token = session[:rtoken].get_access_token(:oauth_verifier => params[:oauth_verifier])
      atoken = access_token.params[:oauth_token]
      asecret = access_token.params[:oauth_token_secret]

      session[:atoken] = atoken
      session[:asecret] = asecret
    elsif !session[:atoken].nil?
      client.access_token = session[:atoken]
      client.access_token_secret = session[:asecret]
      #client.authorize_from_access(session[:atoken], session[:asecret])
    else
      if params[:oauth_secret].nil?
        render :nothing => true, :status => 400
        return
      end

      client.oauth_token = params[:oauth_access_token]
      client.oauth_token_secret = params[:oauth_secret]
      #client.authorize_from_access(params[:oauth_access_token], params[:oauth_secret])
    end

    block.call(client)
  end

end
