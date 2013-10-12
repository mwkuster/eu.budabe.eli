require 'rubygems'
require 'selenium-webdriver'

require 'rdf/rdfa'
require 'rdf/turtle'
require 'rdf/isomorphic'

test_elis_positive = ["/eli/dir_impl/2012/1/oj", "/eli/dir/2012/2/oj", "/eli/dir/2012/3/oj", "/eli/reg_impl/2012/1/oj", "/eli/reg/2012/5/oj"]
test_elis_negative = ["/eli/dir/2012/1/oj", "/eli/reg/2012/2/oj"]


def build_url(eli) 
  domain = "eli.budabe.eu"
  "http://" + domain + eli
end

def check_h1(eli)
  driver = Selenium::WebDriver.for :firefox
  wait = Selenium::WebDriver::Wait.new(:timeout => 30) # seconds

  driver.get build_url eli
  begin
    element = wait.until { driver.find_element(:tag_name, "h1") }
    element.text == "ELI notice for http://eli.budabe.eu" + eli 
  ensure
    driver.quit
  end
end

def write_rdfa(eli)
  graph = RDF::Graph.load(build_url(eli), :format => :rdfa)
  puts "Graph"
  RDF::Turtle::Writer.open(eli.gsub("/", "_") + ".ttl") do |writer|
    writer << graph
  end
end

def check_rdfa(eli)
  graph = RDF::Graph.load(build_url(eli), :format => :rdfa)
  reference_graph = RDF::Graph.load(eli.gsub("/", "_") + ".ttl", :format => :turtle)
  graph.isomorphic_with? reference_graph
end

puts "Positive tests: check_h1"
puts test_elis_positive.select { |eli| !check_h1(eli) }
puts "Negative tests: check_h1"
puts test_elis_negative.select { |eli| check_h1(eli) }
puts "Positive tests: check_rdfa"
puts test_elis_positive.select { |eli| !check_rdfa(eli) }



