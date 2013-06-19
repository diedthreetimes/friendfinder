require 'base64'
require 'openssl'
class AuthorityController < ApplicationController
  respond_to :json
  # Download a signed version of the linked in profile
  def download_profile

    client = LinkedIn::Client.new("your_api_key", "your_secret")
    request_token = client.request_token(:oath_callback => "http://#{request.host_with_port}/auth/callback")
    session[:rtoken] = request_token.token
    session[:rsecret] = request_token.secret
    # TODO: Do we want to set a cookie here? We should probably just use an CookieStore. If so we should stirctly enforce an ssl connection. Or, we could use an encrypted cookiestore of some kind.


    # Load the key
    sk = OpenSSL::PKey::RSA.new(File.open(CaServer::Application.config.sig_key_location).read)
    signature = sk.sign(OpenSSL::Digest::SHA1.new, "Some text to sign")

    ret = {m:"Some text to sign", sig: Base64.encode64(signature) }

    respond_with( ret )
  end
end
