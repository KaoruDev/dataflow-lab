package dataflowlab;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.fs.MoveOptions.StandardMoveOptions;
import org.apache.beam.sdk.io.fs.ResourceId;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.io.ByteStreams;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;
import java.awt.image.BufferedImage;
import org.json.*;


public class ImagesPipeline1 {

	private static final String PROJECT_ID =  "qwiklabs-gcp-dbccb9a43fa6c950";
	private static final String BUCKET_IN_PATH = "gs://"+PROJECT_ID+"-imagesin";
	private static final String BUCKET_OUT_PATH = "gs://"+PROJECT_ID+"-imagesout";
	private static final String TOPIC_URI =  "projects/"+PROJECT_ID+"/topics/iotdata";


	public static class CopyImages extends DoFn<PubsubMessage, String>{

		CopyImages() {}

	    @ProcessElement
	    public void processElement(ProcessContext c) {
			// parse the message and get filePath and label
			String msg = new String(c.element().getPayload());
			JSONObject msgJson = new JSONObject(msg);
			String label = msgJson.getString("label");
			label = label.trim().toLowerCase();
			String pathToFileIn = msgJson.getString("filePath");
			String fileName = "image.jpeg";

			Instant timestamp = Instant.now();
			String pathToFileOut = BUCKET_OUT_PATH + "/" + label + "/" + timestamp.toString() + "-copy-" + fileName;

			List<ResourceId> listIn = new ArrayList<> ();
			List<ResourceId> listOut = new ArrayList<> ();
			listIn.add(FileSystems.matchNewResource(pathToFileIn, false));
			listOut.add(FileSystems.matchNewResource(pathToFileOut, false));
			// copy from in to out
			try {
				FileSystems.copy(listIn, listOut, StandardMoveOptions.IGNORE_MISSING_FILES);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
			c.output(fileName);
	    }

	}



	public static void main(String[] args) {
		// TODO: complete the main method
		// Create PipelineOptions
		PipelineOptions options = PipelineOptionsFactory.fromArgs(args).create();

		// Create Pipeline object
        Pipeline pipeline = Pipeline.create(options);

		// Create PCOllection by reading incoming PubSub message (apply I/O Transform to the pipeline object)
		PCollection<PubsubMessage> pCollection =
			pipeline.apply("read from PubSub", PubsubIO.readMessages().fromTopic(TOPIC_URI));

		// Apply PTransform to the pipeline object with the DoFn class (copy incoming file from bucket in to bucket out)
		pCollection.apply("copy", ParDo.of(new CopyImages()));

		// Run the pipeline
        pipeline.run();
	}

}
