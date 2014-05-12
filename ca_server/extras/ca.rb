require 'openssl'


module CA
  @@sk = OpenSSL::PKey::RSA.new(File.open(CaServer::Application.config.sig_key_location).read)
  @@cert = OpenSSL::X509::Certificate.new( File.open(CaServer::Application.config.cert_location).read )

  def self.sign m, detached = false
    # This should already include the timestamp. However, we may need to manually validate it hasn't expired
    # TODO: Verify the time is included

    flags = OpenSSL::PKCS7::BINARY |
      OpenSSL::PKCS7::NOCERTS |
      OpenSSL::PKCS7::NOSMIMECAP

    if detached
      flags |=  OpenSSL::PKCS7::DETACHED
    end

    OpenSSL::PKCS7::sign(@@cert,
                         @@sk,
                         m,
                         [], flags)
    # We may not want PKCS7_DETACHED to keep the data with the signature
  end
end
