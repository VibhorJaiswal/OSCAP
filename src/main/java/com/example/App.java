import org.dockerjava.api.DockerClient;
import org.dockerjava.api.DockerClientConfig;
import org.dockerjava.api.command.CreateContainerResponse;
import org.dockerjava.api.command.StartContainerCmd;
import org.dockerjava.core.DockerClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OpenScapScanner {

    private static DockerClient dockerClient;

    public static void main(String[] args) {
        // Initialize Docker Client
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().build();
        dockerClient = DockerClientBuilder.getInstance(config).build();

        // Pull the Ubuntu image
        String imageName = "ubuntu:20.04"; // Specify your image version
        pullDockerImage(imageName);

        // Create and start the container
        String containerId = createAndStartContainer(imageName);
        
        // Run the OpenSCAP command
        runOpenScapScan(containerId);

        // Clean up
        stopAndRemoveContainer(containerId);
    }

    private static void pullDockerImage(String imageName) {
        System.out.println("Pulling Docker image: " + imageName);
        dockerClient.pullImageCmd(imageName).exec();
    }

    private static String createAndStartContainer(String imageName) {
        System.out.println("Creating and starting container...");
        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName("ubuntu-scanner-container")
                .exec();
        
        String containerId = container.getId();
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        startContainerCmd.exec();

        return containerId;
    }

    private static void runOpenScapScan(String containerId) {
        System.out.println("Running OpenSCAP scan...");

        try {
            // Execute the oscap command in the container
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", containerId, "oscap", "oval", "eval", "--report", "report.html", "com.ubuntu.focal.usn.oval.xml");
            Process process = processBuilder.start();
            
            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
            System.out.println("OpenSCAP scan completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void stopAndRemoveContainer(String containerId) {
        System.out.println("Stopping and removing container...");
        dockerClient.stopContainerCmd(containerId).exec();
        dockerClient.removeContainerCmd(containerId).exec();
    }
}
