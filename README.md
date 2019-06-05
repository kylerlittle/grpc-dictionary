# grpc-dictionary

## Description
A small project in order to learn the basics of gRPC and refresh my Java & Gradle skills.

The client will have two options. They can either enter a word into the dictionary or they can look up a word in the dictionary. They must provide the [language ISO 639-1 code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) regardless of the option selected. If the user wishes to define a word, then the user will additionally be prompted to enter a definition. The client will then send the word, ISO code, and definition, if applicable, to the server. The server will look up the word if that is requested or add its definition to its storage (JSON file). The server will respond with a status code and definition, if applicable.
