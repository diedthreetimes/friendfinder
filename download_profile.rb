#!/usr/bin/env ruby
require 'oauth'

# Fill the keys and secrets you retrieved after registering your app
api_key = '78imlmkvuvhi'
api_secret = 'BbIEylD3TBcAvVeR'
user_token = '7511b9ec-237a-4790-9707-f0a16b48d721'
user_secret = 'e46f0dd5-1a33-4615-978f-e0fb27859a0b'

# Specify LinkedIn API endpoint
configuration = { :site => 'https://api.linkedin.com',
  # This tells OAuth what URL to use to gain authorization
  :authorize_path => '/uas/oauth/authenticate',
  # THis tells OAuth what URL to use to retrieve a request token
  #   In order to request more specific permisions we append ?scope=r_network
  :request_token_path => '/uas/oauth/requestToken',
  # What URL to get an accessToken
  :access_token_path => '/uas/oauth/accessToken' }

# Use your API key and secret to instantiate consumer object
consumer = OAuth::Consumer.new(api_key, api_secret, configuration)

# Use your developer token and secret to instantiate access token object
access_token = OAuth::AccessToken.new(consumer, user_token, user_secret)

# Make call to LinkedIn to retrieve your own profile
fields = ['first-name', 'headline', 'num-connections'].join(',')
response = access_token.get("http://api.linkedin.com/v1/people/~:(#{fields})")
puts response

connections = access_token.get("http://api.linkedin.com/v1/people/~/connections:(first-name,last-name,id,location)")
#Add {'x-li-format' => 'json} to retrieve json instead

puts connections


#TODO: Handle expired access tokens.


########## This code will allow us to retrieve arbitrary profiles  ################
# This is a hassle so it is commented out for now
=begin
# To retrieve someone elses profile
# First retrieve a request_token
request_token = consumer.get_request_token

# This url will retrieve a PIN that can be used to gain access
# After the User signs in the PIN will be provided, this can be customized to instead redirect to a known location
puts "Request Access URL: " + request_token.authorize_url


#Input the pin from the console
verifier = $stdin.gets.strip

# Use the verifier to gain an access token and proceed as before
#   Our application should save these tokens until they expire
access_token = request_token.get_access_token(:oauth_verifier => verifier)

# API call to retrieve profile using access token
response = access_token.get("http://api.linkedin.com/v1/people/~")
puts response
=end
