require 'hbc_psi'
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
      respond_with( c.profile )
    end
  end

  def download_connections
    with_log_in do |c|
      # @profile = client.profile

      friends = c.connections[:all]

      resp = {}
      resp[:connections] = friends

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
    if session[:atoken].nil?
      if params[:oauth_verifier].nil?
        redirect_to action: 'login', format: request.format.to_sym
        return
      end

      pin = params[:oauth_verifier]
      atoken, asecret = client.authorize_from_request(session[:rtoken], session[:rsecret], pin)
      session[:atoken] = atoken
      session[:asecret] = asecret
    else
      client.authorize_from_access(session[:atoken], session[:asecret])
    end

    block.call(client)
  end

end
