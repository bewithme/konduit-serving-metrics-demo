package konduit.demo.metrics;

import ai.konduit.serving.InferenceConfiguration;
import ai.konduit.serving.config.ServingConfig;
import ai.konduit.serving.config.metrics.impl.ClassificationMetricsConfig;
import ai.konduit.serving.deploy.DeployKonduitServing;
import ai.konduit.serving.pipeline.step.ImageLoadingStep;
import ai.konduit.serving.pipeline.step.model.KerasStep;
import com.jayway.restassured.response.Response;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.nd4j.common.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.File;

import static ai.konduit.serving.metrics.MetricType.*;
import static com.jayway.restassured.RestAssured.given;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {

        String inputName = "input";

        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
                .servingConfig(ServingConfig.builder()
                        .httpPort(9008)
                        .listenHost("0.0.0.0")
                        .metricTypes(Arrays.asList(CLASS_LOADER, JVM_MEMORY, JVM_GC, PROCESSOR, JVM_THREAD, LOGGING_METRICS, NATIVE, CLASSIFICATION))
                        .metricsConfigurations(Collections.singletonList(
                                new ClassificationMetricsConfig(Arrays.asList(
                                        "Number_0", "Number_1", "Number_2",
                                        "Number_3", "Number_4", "Number_5",
                                        "Number_6", "Number_7", "Number_8",
                                        "Number_9"))))
                        .build())
                .step(ImageLoadingStep.builder()
                        .inputName(inputName)
                        .imageProcessingInitialLayout("NCHW")
                        .imageProcessingRequiredLayout("NHWC")
                        .dimensionsConfig(inputName, new Long[] { 28L, 28L, 1L })
                        .outputName("output")
                        .build())
                .step(KerasStep.builder()
                        .inputName(inputName)
                        .outputName("dense2")
                        .path(new File("models/keras_mnist_model.h5").getAbsolutePath())
                        .build())
                .build();

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions()
                .setMicrometerRegistry(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
                .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
        BackendRegistries.setupBackend(micrometerMetricsOptions);

        VertxOptions vertxOptions = new VertxOptions().setMetricsOptions(micrometerMetricsOptions)
                .setMetricsOptions(micrometerMetricsOptions).setMaxEventLoopExecuteTime(1000000L).setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);

        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(new JsonObject(inferenceConfiguration.toJson()));

        DeployKonduitServing.deployInference(vertxOptions, deploymentOptions,
                handler -> {
                    if(handler.succeeded()) {
                        Random random = new Random(System.currentTimeMillis());

                        int numberOfRequests = 1000;
                        
                        long startTime=System.currentTimeMillis();
                        for (int i = 0; i < numberOfRequests; i++) {
                            int label = random.nextInt(
                                    1 + new Random(System.currentTimeMillis()).nextInt(
                                            1 + new Random(System.currentTimeMillis()).nextInt(10)));

                            try {
                                Response response = given().port(9008).multiPart(inputName,
                                        new File( String.format("test_files/test_input_number_%s.png", label)))
                                        .post("/CLASSIFICATION/IMAGE").andReturn();
                                System.out.println("Actual label: " + label + System.lineSeparator() + response.asString());

                              //  Thread.sleep(30);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        long endTime=System.currentTimeMillis();
                        
                        System.out.println("*** Completed all inferences at "+(endTime-startTime)/(1000)+" S");
                        
                    } else {
                        handler.cause().printStackTrace();
                    }
                });
    }
}
