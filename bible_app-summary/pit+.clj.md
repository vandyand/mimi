# Bible App Pit

## Function Definitions

### parse-response-body
This function takes a response map and extracts the body from it. It then parses the body as JSON and converts the keys to keywords.

### post-gpt
This function sends a POST request to the OpenAI GPT-3 API to generate completions. It includes the necessary headers and body parameters for the request.

### p+
This function takes an argument and prints it to the console. It then returns the argument.

### query-gpt
This function takes a query and performs a sequence of operations using the previous functions. It posts the query to the GPT-3 API, parses the response body, and extracts the content of the first choice.

### gen-agent-body
This function generates a body for an agent by calling the `query-gpt` function with a specific prompt.

### gen-agent
This function generates an agent by creating a map with an ID and a body. If no body is provided, it generates a default body by calling `gen-agent-body`.

### gen-agents
This function generates a list of agents by calling `gen-agent` with a list of bodies.

### mutate-agent
This function mutates an agent by calling `query-gpt` with a specific prompt to improve the agent's prompt.

### perform
This function performs a task by calling `query-gpt` with a specific prompt and the agent's body.

### get-task-info
This function retrieves task information by calling an external API with a book, chapter, and verse parameters.

### get-next-task-info
This function retrieves the next task information by calling `get-task-info` with the next book, chapter, and verse parameters.

### battle
This function performs a battle between two summaries by calling `query-gpt` with specific prompts and the task information.

## Data Generation

1. Generate a list of agents by calling `gen-agents` with a list of agent bodies.
2. Loop through the agents and perform challenges:
   - Get a random agent pair from the shuffled agent list.
   - Get the task information for the current reference.
   - Perform the task for each agent in the pair.
   - Perform a battle between the two summaries.
   - Select the winner and mutate the agent to generate a child agent.
   - Add the child agent to the agent list and remove the loser agent.
   - Get the next verse reference.
   - Repeat the loop until the maximum number of challenges is reached or an error occurs.

## Bible Books

The list of Bible books contains the names of all the books in the Bible.

## Summary

This file contains functions for generating and evaluating agents for the Bible App Pit challenge. It uses the OpenAI GPT-3 API to generate completions based on prompts and performs battles between agents to select winners and generate new agents.