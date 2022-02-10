package configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CustomShardedCreation {
    public static void main(String[] args) {
        int partitions = 1;
        short replicationFactor = 1;
        List<NewTopic> topics = new ArrayList<>();

        String uri1 = "mongodb://137.121.170.248:31184";
        String uri2 = "mongodb://137.121.170.248:31185";

        System.out.println("Create collections on Mongo1");
        MongoClient mongoClient = MongoClients.create(uri1);
        MongoDatabase database = mongoClient.getDatabase("test");
        try {
            Scanner sc = new Scanner(new File("balanced_areas_2_1.txt"));
            String line;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                database.createCollection(line);
                topics.add(new NewTopic(line, partitions, replicationFactor));
            }
            mongoClient.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Create collections on Mongo2");
        mongoClient = MongoClients.create(uri2);
        database = mongoClient.getDatabase("test");
        try {
            Scanner sc = new Scanner(new File("balanced_areas_2_2.txt"));
            String line;
            while(sc.hasNextLine()) {
                line = sc.nextLine();
                database.createCollection(line);
                topics.add(new NewTopic(line, partitions, replicationFactor));
            }
            mongoClient.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Properties properties = new Properties();
        properties.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                "137.121.170.248:31090,137.121.170.248:31091,137.121.170.248:31092"
        );

        try (Admin admin = Admin.create(properties)) {
            CreateTopicsResult result = admin.createTopics(topics);

            KafkaFuture<Void> future = result.values().get(topics.get(topics.size()-1));
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
