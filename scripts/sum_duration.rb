#!/usr/bin/ruby

# prints the sum of the durations of all operations

if ARGV.length < 1
  print "Usage: #{__FILE__} <to discard threshold> (in seconds)\n"
  exit 1 
end

threshold = ARGV[0].to_i

total_duration = 0

$stdin.each do |line|
  duration = (line.split("\t")[1].split("-")[1].to_i) / 1000000.0

  if duration < threshold 
    total_duration += duration
  else
    $stderr.puts "WARNING: #{duration} is greater than threshold (#{threshold})"
  end
end
  print "##########################################\n"
  print "#{total_duration} secs\n"
