package chapter6.hcat;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hcatalog.data.HCatRecord;
import org.apache.hcatalog.mapreduce.HCatInputFormat;

public class HCatReadMapReduce extends Configured implements Tool {

	public static class UserReadMapper extends
			Mapper<WritableComparable, HCatRecord, IntWritable, IntWritable> {

		IntWritable ONE = new IntWritable(1);

		@Override
		public void map(
				WritableComparable key,
				HCatRecord value,
				Mapper<WritableComparable, HCatRecord, IntWritable, IntWritable>.Context context)
				throws IOException, InterruptedException {

			int age = (Integer) value.get(2);
			// emit age and one for count
			context.write(new IntWritable(age), ONE);
		}
	}

	public static class Reduce extends
			Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

		public void reduce(
				IntWritable key,
				Iterable<IntWritable> values,Context context)
				throws IOException, InterruptedException {
			if (key.get() < 34 & key.get() > 18) {
				int count = 0;
				for (IntWritable val : values) {
					count += val.get();
				}
				context.write(key, new IntWritable(count));
			}
		}
	}

	public int run(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage:  <dbname> <tablename> <outpath>");
			System.exit(-1);
		}
		/* input parameters */
		String dbName = args[0];
		String tableName = args[1];
		String outputPath = args[2];

		Job job = Job.getInstance(getConf(), "HCatMapReduceSample");
		job.setJarByClass(HCatReadMapReduce.class);
		job.setMapperClass(UserReadMapper.class);
		job.setReducerClass(Reduce.class);

		// Set HCatalog as the InputFormat
		job.setInputFormatClass(HCatInputFormat.class);
		HCatInputFormat.setInput(job, dbName, tableName);

		// Mapper emits a string as key and an integer as value
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);


		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(IntWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		int exitStatus = job.waitForCompletion(true) ? 0 : 1;
		return exitStatus;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(),
				new HCatReadMapReduce(), args);
		System.exit(res);
	}
}