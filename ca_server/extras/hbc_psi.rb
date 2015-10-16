#Honest but curious two way authroized psi
require 'ca'
require 'digest/sha1'
class HbcPsi
  cattr_accessor :g, :q, :p
  @@g= <<-'EOS'.split.join.hex
    D29D5121 B0423C27
    69AB2184 3E5A3240 FF19CACC 792264E3 BB6BE4F7 8EDD1B15
    C4DFF7F1 D905431F 0AB16790 E1F773B5 CE01C804 E509066A
    9919F519 5F4ABC58 189FD9FF 987389CB 5BEDF21B 4DAB4F8B
    76A055FF E2770988 FE2EC2DE 11AD9221 9F0B3518 69AC24DA
    3D7BA870 11A701CE 8EE7BFE4 9486ED45 27B7186C A4610A75
  EOS
  @@q= <<-'EOS'.split.join.hex
    E950511E AB424B9A 19A2AEB4 E159B784 4C589C4F
  EOS

  @@p= <<-'EOS'.split.join.hex
    E0A67598 CD1B763B
    C98C8ABB 333E5DDA 0CD3AA0E 5E1FB5BA 8A7B4EAB C10BA338
    FAE06DD4 B90FDA70 D7CF0CB0 C638BE33 41BEC0AF 8A7330A3
    307DED22 99A0EE60 6DF03517 7A239C34 A912C202 AA5F83B9
    C4A7CF02 35B5316B FC6EFB9A 24841125 8B30B839 AF172440
    F3256305 6CB67A86 1158DDD9 0E6A894C 72A5BBEF 9E286C6B
  EOS

  @@rc= <<-'EOS'.split.join.hex
    B30426F 062E6421A 0D2F74D0 FA79D8AD 66C1325A
  EOS

  @@t = (p-1)/q

  def self.sig_message(set, protocol)
    #TODO: Ensure set is unique and flat

    ru = bigrand(512) % @@q

    if protocol.nil? || protocol=="psi"
      plaintext_set = set.collect do |x|
        self.modpow(hash_str(x, "\0"), ru, @@p) 
      end
    elsif protocol=="psi_ca_dep" # old psi ca version

      plaintext_set = set.collect do |x|
        self.modpow(hash_str(x, "\0"), ru, @@p) 
      end

      rus = bigrand(512) % @@q
      shuffle_set = set.shuffle(random: SecureRandom.hex(23).to_i(16))
      plaintext_shuffle_set = shuffle_set.collect do |x|
        hash_str( self.modpow(hash_str(x, "\0"), rus, @@p).to_s(16), "\1")
      end

      plaintext_set += plaintext_shuffle_set; # append plain text

    elsif protocol=="psi_ca"
      shuffle_set = set.shuffle(random: SecureRandom.hex(23).to_i(16))
      plaintext_set = shuffle_set.collect do |x|
        self.modpow(self.modpow(hash_str(x, "\0"), ru, @@p), @@rc, @@p)
      end
    else
      raise RuntimeError, 'Undefined protocol'
        
    end

    string_set = plaintext_set.collect do |x|
      encode(x)
    end.join(' ')
      
    plaintext = plaintext_set.collect do |x|
      x.to_s(16)
    end.join(' ');

    sig = CA.sign(string_set)
    # Do we want to include the data in the signature? It seems that it's encoding is smaller
    # DEBUG PURPOSES ONLY

    if protocol=="psi_ca_dep"
      {signed_message: sig.to_pem, secret: encode( ru ) + ' ' + encode( rus ), plaintext: plaintext,  p: @@p.to_s(16), q: @@q.to_s(16), g: @@g.to_s(16), t: @@t.to_s(16) }
    else
      {signed_message: sig.to_pem, secret: encode( ru ), plaintext: plaintext,  p: @@p.to_s(16), q: @@q.to_s(16), g: @@g.to_s(16), t: @@t.to_s(16) }
    end
    # {signed_message: sig.to_pem, secret: encode( ru )}

  end

  def self.encode bn
    # Radix 16 print the bn
    # Then convert to hex high nible first
    # Finally base64 encode
    hexstr = bn.to_s(16)

    pad = (hexstr.size % 2 == 0) ? "" : "0"
    [[pad + hexstr].pack("H*")].pack("m")
  end

  # Compute a^n mod m
  def self.modpow(a, n, m)
    r = 1
    while true
      r = r * a % m if n[0] == 1
      n >>= 1
      return r if n == 0
      a = a * a % m
    end
  end

  # Selector should be a binary string
  def self.hash_str(s, selector = "\0")
    modpow(Digest::SHA1.hexdigest(selector+s.to_s()).hex, @@t, @@p)
  end

  def self.bigrand(bytes)
    OpenSSL::Random.random_bytes(bytes).unpack("H*")[0].hex
  end

end
