require 'base64'
require 'openssl'


module CA
  @@sk = OpenSSL::PKey::RSA.new(File.open(CaServer::Application.config.sig_key_location).read)

  def self.sign m
    signature = @@sk.sign(OpenSSL::Digest::SHA1.new, m)
    Base64.encode64(signature)
  end
end
