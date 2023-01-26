This application if for the only purpose of building a 'sample' generic pool java class of resources R (GenericPool<R>) 
from wich perform asynchronously operations, like 

    - add(R) -> boolean
    - acquire() -> R
    - release(R) -> boolean
    - remove(R) -> boolean
    - ..

The essential problem here, is how to update shared data between threads, avoiding racing conditions and potential
unconsistent states.

For that we choose Java concurrent lock API, including WriteLock and Condition classes.

Condition is an essential part of shared data between threads control. Whenever in a running thread happens some
'condition' (tipically a 'wait for something in the future to happen, some state change / new data to show up') the Condition class 
stop blocking the thread (eventually with some timeout or forever) waiting for some other thread to fullfill that condition asynchronously.

The application includes three tests (the latest an interesting async test), and also a kind of 'stress' test class (StressTest.java) 
wich bombards de GenericPool with add, acquire, release and remove operations randomly at 1 second interval, hoping for 
some exception / bad behavior to occur, not being detected so far.

Hope you enjoy




