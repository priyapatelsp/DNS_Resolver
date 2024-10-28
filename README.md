
<h1>DNS Resolver</h1>

This challenge is to build your own DNS Resolver .
This tool is build as part of <a href="https://codingchallenges.fyi/challenges/challenge-dns-resolver">coding challenges</a> .


```
A DNS (Domain Name System) resolver is an essential element of the DNS infrastructure,responsible for translating
human-readable domain names (e.g., www.example.com) into their corresponding IP addresses (e.g., 192.0.2.1).
This server processes domain name queries from clients, such as web browsers, and returns the appropriate IP addresses,
enabling users to access websites and online services efficiently.
```

<h1>Get started </h1>
This tool is in Java , so please ensure that you have latest version installed in your device 
<br><br>

step 1: git clone https://github.com/priyapatelsp/DNS_Resolver.git

step 2: Open folder and inside run DNSClient.java

you can change your own root server IP:

```
ROOT_DNS_SERVER = "<ANY ROOT SERVER FROM ROOT SERVER LIST >";
```

you will get the output similar to 

```
Querying 192.5.5.241 for dns.google.com
Response ID: 55143
Is Response: true
Questions: 1
Answers: 0
Authorities: 0
Additionals: 0
```

<h2>Helpful resources </h2>
<a href="https://www.iana.org/domains/root/servers">Root server list </a> <br>
<a href ="https://en.wikipedia.org/wiki/Root_name_server"> Root server</a>


<h1>Author</h1><br>
Priya Patel <br>
Github : <a href="https://github.com/priyapatelsp">priyapatelsp</a>
