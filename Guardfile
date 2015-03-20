require 'asciidoctor'
require 'erb'

options = {:mkdirs => true, :safe => :unsafe, :attributes => ['linkcss', 'allow-uri-read']}

guard 'shell' do
  watch(/^[A-Z-a-z][^#]*\.adoc$/) {|m|
    Asciidoctor.render_file('src/main/asciidoc/README.adoc', options.merge(:to_file => './README.md'))
    Asciidoctor.render_file('src/main/asciidoc/spring-cloud-cluster.adoc', options.merge(:to_dir => 'target/generated-docs'))
  }
end
