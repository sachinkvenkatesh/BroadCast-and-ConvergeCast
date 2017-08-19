# Broadcast and Converge cast

## Spanning Tree (Distributed algo) construction
Each node sends a request to its neighbors. Each node on receiving a request sets/checks a flag. The first request received from a node becomes the parent (send ACK) and for all the rest send NACK. A node receiving the ACK, the sender becomes its children. By this message exchange, a spanning tree is constrcted.

## Broadcast and Convergecast
Message - Broadcast message and Reply message
Broadcast message has the message source node id, intermediate node id and message content.
Reply message has the message source node id and intermediate node id.
A map with message source node id as key and itermediate node id as value is stored at each node on receving a broadcast message.
On receiving the convergecast, check the map with the source node id, get the intermediate source and send Reply message to that node. Remove this entry from the map.
Limitation : At a given time a node can broadcast only one message and on receiving the convergecast message for that broadcast, the node can go for next message. However, multiple nodes can broadcast at the same time. To make multiple broadcast from multiple nodes simultaneously, a small change in the map can be done. Modify the key as (source node id and message-id) and have message-id field in each message.

## Testing
A random number was generated and broadcasted with the message. Add the number at each node. At the end of all broadcast and convergecast from each node, the sum at each node must be the same.

