require 'base64'
require 'openssl'
class AuthorityController < ApplicationController
  respond_to :json
  # Download a signed version of the linked in profile
  def download_profile
    logger.debug CaServer::Application.config.sig_key_location
    # Load the key
    sk = OpenSSL::PKey::RSA.new(File.open(CaServer::Application.config.sig_key_location).read)
    signature = sk.sign(OpenSSL::Digest::SHA1.new, "Some text to sign")

    ret = {m:"Some text to sign", sig: Base64.encode64(signature) }

    respond_with( ret )
  end
end
