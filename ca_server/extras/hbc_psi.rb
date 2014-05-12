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

  @@t = (p-1)/q

  def self.sig_message set
    #TODO: Ensure set is unique and flat

    ru = bigrand(512) % @@q

    string_set = set.collect do |x|
      encode( self.modpow(hash_str(x), ru, @@p) )
    end.join(' ')

    sig = CA.sign(string_set)
    # Do we want to include the data in the signature? It seems that it's encoding is smaller
    {msg: string_set, sig: sig.to_pem, secret: encode( ru )}

  end

  def self.encode bn
    [[bn.to_s(16)].pack("H*")].pack("m")
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
    modpow(Digest::SHA1.hexdigest(selector+s).hex, @@t, @@p)
  end

  def self.bigrand(bytes)
    OpenSSL::Random.random_bytes(bytes).unpack("H*")[0].hex
  end

end
