#!/bin/bash

# Interactive MCP Test Script for CRE
# project_root defaults to /home/hainn/blue/code/cre-test-project

PROJECT_ROOT="/home/hainn/blue/code/cre-test-project"
JAR="target/cre-0.1.0-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found. Building..."
    mvn clean package -DskipTests
fi

echo "--- CRE Interactive Tester ---"
echo "Project Root: $PROJECT_ROOT"
echo "1. get_context (searchBooks)"
echo "2. find_implementations (BookSearchService)"
echo "3. trace_flow (searchBooks)"
echo "4. reset_project"
echo "5. Custom node_id (get_context)"
echo "q. Quit"

read -p "Choose: " choice

case $choice in
    1) JSON='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_context","arguments":{"project_root":"'$PROJECT_ROOT'","node_id":"com.bookstore.controller.BookSearchController.searchBooks(String,String,String)"}},"id":1}' ;;
    2) JSON='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"find_implementations","arguments":{"project_root":"'$PROJECT_ROOT'","interface_fqn":"com.bookstore.service.BookSearchService"}},"id":1}' ;;
    3) JSON='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"trace_flow","arguments":{"project_root":"'$PROJECT_ROOT'","entry_method_node_id":"com.bookstore.controller.BookSearchController.searchBooks(String,String,String)"}},"id":1}' ;;
    4) JSON='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"reset_project","arguments":{"project_root":"'$PROJECT_ROOT'"}},"id":1}' ;;
    5) read -p "node_id: " nid; JSON='{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_context","arguments":{"project_root":"'$PROJECT_ROOT'","node_id":"'$nid'"}},"id":1}' ;;
    q) exit 0 ;;
    *) echo "Invalid"; exit 1 ;;
esac

echo "Executing: $JSON"
echo "$JSON" | java -jar "$JAR"
