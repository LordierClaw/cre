import json
import subprocess
import os

PROJECT_ROOT = "/home/hainn/blue/code/cre-test-project"
JAR = "target/cre-0.1.0-SNAPSHOT.jar"

def call_mcp(method, params):
    payload = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params,
        "id": 1
    }
    process = subprocess.Popen(
        ["java", "-Dmcp.transport=stdio", "-jar", JAR],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    stdout, stderr = process.communicate(input=json.dumps(payload))
    try:
        if not stdout:
            return None
        return json.loads(stdout)
    except Exception as e:
        return None

def test_format():
    print("Testing output format...")
    params = {
        "name": "get_context",
        "arguments": {
            "project_root": PROJECT_ROOT,
            "node_id": "com.bookstore.controller.AdminBookController::createBook(BookRequest)",
            "depth": 0
        }
    }
    result = call_mcp("tools/call", params)
    if not result or "result" not in result:
        print("FAIL: No result from MCP")
        return False
    
    content = result["result"]["content"][0]["text"]
    
    # Assertions
    has_file_tag = '<file name="com.bookstore.controller.AdminBookController">' in content
    has_legacy_tag = '<AdminBookController>' in content
    
    if has_file_tag and not has_legacy_tag:
        print("PASS: Format migration successful")
        return True
    else:
        if not has_file_tag:
            print("FAIL: Missing <file name=\"...\"> tag")
        if has_legacy_tag:
            print("FAIL: Legacy <AdminBookController> tag still present")
        return False

if __name__ == "__main__":
    if test_format():
        exit(0)
    else:
        exit(1)
