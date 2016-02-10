require 'ca'
require 'digest/sha1'
class HbcBPsiCa < HbcPsi

  @@secret = "\0"

  # simple way to do this.
  def self.issue_capabilities(set)

    # we dont need to shuffle because we need to match this with original order
    #shuffle_set = set.shuffle(random: SecureRandom.hex(23).to_i(16))
    plaintext_set = set.collect do |x|
      hash_str(x, @@secret)
    end

    string_set = plaintext_set.collect do |x|
      encode(x)
    end.join(' ')

    plaintext = plaintext_set.collect do |x|
      x.to_s(16)
    end.join(' ');

    sig = CA.sign(string_set) # probably dont need it but for consistency.

    # TODO: add timestamp?

    {signed_message: sig.to_pem, plaintext: plaintext }
  end

end
