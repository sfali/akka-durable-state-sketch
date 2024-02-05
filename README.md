# DeliveryDateService

 This service is intended to be an a simple demonstration of how to use the Akka Durable State library to create
persistent cluster sharded entities without relying on Cassandra as an event journal but rather Postgres as a storage of State. The service will also implement a CQRS style architecture
where we seperate the write-side of the service from the read-side query of the data. The service will also be presumed to live as a microservice where it consumes events from the outside via 
a kafka topic and then produces domain events to an kafka egress topic for downstream services to consume. 


### Domain
 The basic entity of this service is the `DeliveryDateEntity` which is a durable state entity representing the current delivery
date for a package. Whenever new events are generated against the packageId the system updates its DeliveryDate making it more
accurate. These events are represented by an `EventId` which is a 4 digit number representing some kind of external processing that the package went through. Based on a series of rules as these events are applied to the package, the delivery date gets closer and in this made-up scenario, more accurate as well. 



### How to run
1. `docker-compose up` this will start the Kafka, Zookeeper and Postgres instances. It will create the egress/ingress topics as well as create the various tables needed.
2.  Run the DeliveryDateApp 
3. Open a terminal to Kafka to gain access to the KafkaCLI and run the following command to open the producer shell script
   `kafka-console-producer --topic external-events --bootstrap-server localhost:9092`
4. Paste in the appropriate payload, an example is `{ "packageId": "28370fa8-024d-467f-97d6-fbdbfa25f250", "eventId": 4000 }` this represents the package having recieved a "4000 event" which in the real world could mean its been scanned at a local sorting facility, or made it to its final destination facility.
5. There is a http server running with a helper API in order to access the current DeliveryDate for a package, an example would be
 `GET http://localhost:1234/packageId/28370fa8-024d-467f-97d6-fbdbfa25f250` this will present a string of the current eventId processed, the current delivery date and a log of all previous events that the package has experienced. 


### Diagram
This system diagram shows the various processes running within the application. We have a seperate write-side representing a Kafka Consumer that calls the DeliveryDateService API and the updated State is persisted in Postgres once the payload as been validated. Also we write events to a Journal which exists in Postgres which then is projected directly to Kafka on the egress topic for downstream consumes.  For the read-side we have the ability to directly query the state from the journal which is wrapped inside an HTTP GET call
<img width="1256" alt="image" src="https://github.com/davidleacock/akka-durable-state-sketch/assets/125934110/05d611b5-1037-4725-82f5-8e50eb673250">


Using `DurableStateBehaviour` in conjuntion with a `changeEventHandler` we get to build a read model as we would with a standard event-sourced behaviour actor, but using Postgres rather than Cassandra. 

<img width="530" alt="image" src="https://github.com/davidleacock/akka-durable-state-sketch/assets/125934110/3fa3ebd3-e68b-43b5-badb-06a7d078c2cb">
