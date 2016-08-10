This is code for Single Threaded NIO based LRU 

#How to Run

Run the CacheServer.java as java application in your favourite IDE.

This will start Server thread and listen on 8080 port by default.

Run CacheClient.java as java application. This starts 2 client threads and sends PUT/GET requests in random order ( basically one after other). 
Each PUT request uses a random number between 1-110 and LRU is configured for 100 elements. I have tested with over 1000000 without any issue. 
However, larger values of random keys will make the system return null for GET calls until most of the numbers are exhausted. 

#Capabilities

This demonstrates following:
1) Server is single threaded and based on Selector for true NIO.
2) Client uses polling with ByteBuffer and uses Channel semantics
3) Sticks to defined protocol and demonstrates packet assembly over connection which can send any amount of data during read/write.
4) USes single threaded server hence removing the condition for explicit synchronization.

#What is missing

1) Not a true server !!
2) No error recovery (basic protection against random command other than PUT/GET) , 
3) No fault tolerance.



