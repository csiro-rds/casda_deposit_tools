require 'optparse'

options = {}
optparse = OptionParser.new do |opts|
  opts.banner = "Convert a component catalogue file so that the component ids contain the SBID\nUsage: convert_component_catalogue_component_ids.rb [options]"

  opts.on("-o", "--old_sbid [OLD_SBID]", "The old observation SBID that already forms a part of the component id") do |old_sbid|
    options[:old_sbid] = old_sbid || ""
  end
  opts.on("-s", "--sbid SBID", "The observation SBID") do |sbid|
    options[:sbid] = sbid || ""
  end
  opts.on("-c", "--catalogue CATALOGUE", "The component catalogue filename") do |catalogue|
    options[:catalogue] = catalogue || ""
  end
	opts.on_tail("-h", "--help", "Show this message") do
      puts opts
      exit 0
    end
end
begin
	optparse.parse!
	raise OptionParser::ParseError.new if options[:sbid].nil? || options[:catalogue].nil?
rescue OptionParser::ParseError
	puts optparse
	exit 1
end

filename = options[:sbid] + "/" + options[:catalogue]

converted = false

require "rexml/document"
file = File.new(filename)
doc = REXML::Document.new(file)

doc.root.elements['RESOURCE'].elements['TABLE'].elements["PARAM[@name='imageFile']"].attributes['value'] = doc.root.elements['RESOURCE'].elements['TABLE'].elements["PARAM[@name='imageFile']"].attributes['value'].split("/").last
component_id_index = 0
component_id_field_found = false
component_id_arraysize = nil
doc.root.elements['RESOURCE'].elements['TABLE'].elements.each("FIELD") do |e|
    if e.attributes['name'] == 'component_id'
        component_id_field_found = true
        component_id_arraysize = e.attributes['arraysize']
        raise "Unknown arraysize for component_id FIELD '#{component_id_arraysize}'" unless /\d+\*|\d+|\*/ =~ component_id_arraysize
        break
    else
        component_id_index += 1
    end
end
if component_id_field_found
    doc.root.elements['RESOURCE'].elements['TABLE'].elements.each("DATA/TABLEDATA/TR") do |tr|
        index = 0
        tr.elements.each("TD") do |td|
            if index == component_id_index
                component_id = td.text
                new_component_id = component_id.chomp.strip
                if /^#{options[:sbid]}-.*$/ =~ new_component_id
                    # Already converted
                else
                    new_component_id = new_component_id.sub(/^_Images\//, '') # Strip old Selavy stuff
                    if options[:old_sbid] && /^#{options[:old_sbid]}-.*$/ =~ new_component_id
                        new_component_id = new_component_id.strip.sub(/^#{options[:old_sbid]}-/, "#{options[:sbid]}-")
                    else
                        new_component_id = options[:sbid] + "-" + new_component_id.strip
                    end
                    case component_id_arraysize
                    when /\d+/
                        if new_component_id.size > component_id_arraysize.to_i
                            raise "SBID is too large to be written into the component id in #{filename} (was '#{component_id}' and trying to make it '#{new_component_id}' - arraysize is #{component_id_arraysize})"
                        end
                        new_component_id = " " * (component_id_arraysize.to_i - new_component_id.size) + new_component_id
                    when "*"
                        # Doesn't matter what the length is
                    when /\d+\*/
                        if new_component_id.size > component_id_arraysize.sub("*", "").to_i
                            raise "SBID is too large to be written into the component id in #{filename} (was '#{component_id}' and trying to make it '#{new_component_id}' - arraysize is #{component_id_arraysize})"
                        end
                        # No need to pad
                    else
                      raise "Illegal arraysize for component_id FIELD '#{component_id_arraysize}'"
                    end
                    new_text = td.text.sub(td.text.chomp.strip, new_component_id)
                    converted = (td.text != new_text)
                    td.text = new_text 
                end
            end
            index += 1
        end
    end
end

File.open(filename + ".converting", "w") do |file|
  file.write(doc)
end

if converted
	`cp #{filename} #{filename}.$(date +"%Y%m%d.%H%M")`
	`mv #{filename}.converting #{filename}`
	puts "File #{filename} had its 'component_id's sucessfully converted"
else
	puts "File #{filename} NOT converted (it may not be a component catalogue file or it may already have been converted)"
end
