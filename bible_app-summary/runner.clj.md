# Bible App Runner

## Functions

### parse-response-body

This function takes a `response` as input and reads the body of the response using the `json/read-str` function, converting it to a keyword map.

### post-gpt

This function takes a `query` as input and sends a POST request to the OpenAI API with the GPT-3.5-turbo model. It includes the query as a user message in the request body. It returns the response body.

### p+

This function takes an argument `x` and prints it. It then returns the argument unchanged.

### query-gpt

This function takes a `query` as input. It calls the `post-gpt` function with the query, then calls the `parse-response-body` function on the response body. It then accesses the content of the first choice message and returns it.

### gen-agent-body

This function generates a query requesting a sentence of random words without any AI voice, description, or summary. It calls the `query-gpt` function with the generated query and returns the result.

### gen-agent

This function generates an agent object with a unique ID and the provided `body` as its content. If no `body` is provided, it generates a random one using the `gen-agent-body` function.

### gen-agents

This function takes a list of `bodies` as input and generates a list of agent objects using the `gen-agent` function for each body.

### gen-agent-bodies

This function generates a list of agent bodies by calling the `gen-agent-body` function a specified number of times.

## Usage

This file provides functions for interacting with the Bible App runner. Use the provided functions to generate agent objects with specific bodies or generate a list of agent objects with random bodies.