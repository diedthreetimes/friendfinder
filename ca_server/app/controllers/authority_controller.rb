require 'hbc_psi'
require 'ca'

class AuthorityController < ApplicationController
  respond_to :json, :html
  def login
    #TODO: Store the redirected url in the session and then use it again here
    request_token = client.request_token(:oauth_callback => "http://#{request.host_with_port}/authority/download_connections.#{request.format.to_sym}")
    session[:rtoken] = request_token.token
    session[:rsecret] = request_token.secret


    puts request.format.to_sym

    redirect_to request_token.authorize_url
    # TODO: Do we want to set a cookie here? We should probably just use an CookieStore. If so we should stirctly enforce an ssl connection. Or, we could use an encrypted cookiestore of some kind.
  end

  def index
    respond_with( {success:"True"} )
  end

  def logout
    session[:atoken] = nil
    redirect_to action: 'index'
  end

  def download_profile
    with_log_in do |c|
      respond_with( CA.sign(c.profile.to_json, false).to_pem )
    end
  end

  def download_connections
    with_log_in do |c|
      connections = c.connections[:all]
      private_friends = connections.select{|f| f.first_name == "private"}
      friends = connections.select{|f| f.first_name != "private"}

      resp = {}
      resp[:count] = friends.count
      if !params[:include_connections].nil?
        resp[:connections] = friends
        resp[:privates] = private_friends.count
      end
      #resp[:connections] = c.profile( id: friends.first["id"], fields: %w(positions) )

      resp[:psi_message] = HbcPsi.sig_message(friends.collect {|v| v[:id]})

      respond_with( resp )
    end
  end

  # TODO: Give a way to download certificate securely. We would need a non self signed certificate to really be secure.
  # TODO: ensure HTTPS only

  private
  def client
    @client ||= LinkedIn::Client.new("78imlmkvuvhi", "BbIEylD3TBcAvVeR")
  end

  def with_log_in &block
    if session[:atoken].nil? && params[:oauth_access_token].nil?
      if params[:oauth_verifier].nil?
        redirect_to action: 'login', format: request.format.to_sym
        return
      end

      puts "Before HA"
      pin = params[:oauth_verifier]
      atoken, asecret = client.authorize_from_request(session[:rtoken], session[:rsecret], pin)

      session[:atoken] = atoken
      session[:asecret] = asecret
    elsif !session[:atoken].nil?
      client.authorize_from_access(session[:atoken], session[:asecret])
    else
      if params[:oauth_secret].nil?
        render :nothing => true, :status => 400
        return
      end

      client.authorize_from_access(params[:oauth_access_token], params[:oauth_secret])
    end

    block.call(client)
  end

end
