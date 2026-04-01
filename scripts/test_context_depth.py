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
        ["java", "-Dmcp.transport=stdio", "-Dlogging.level.root=WARN", "-jar", JAR],
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
    except:
        return None

def test_depth_0():
    print("Testing depth=0 (Strict start node only)...")
    params = {
        "name": "get_context",
        "arguments": {
            "project_root": PROJECT_ROOT,
            "node_id": "com.bookstore.controller.AdminBookController::createBook(BookRequest)",
            "depth": 0
        }
    }
    result = call_mcp("tools/call", params)
    content = result["result"]["content"][0]["text"]
    
    # Should contain createBook, but NOT getBook or getAllBooks
    has_target = "createBook" in content
    has_other = "getBook" in content or "getAllBooks" in content
    
    if has_target and not has_other:
        print("PASS: Depth 0 is strict")
        return True
    else:
        print("FAIL: Depth 0 contains irrelevant methods")
        return False

def test_depth_1():
    print("Testing depth=1 (Target + direct callees)...")
    params = {
        "name": "get_context",
        "arguments": {
            "project_root": PROJECT_ROOT,
            "node_id": "com.bookstore.controller.AdminBookController::createBook(BookRequest)",
            "depth": 1
        }
    }
    result = call_mcp("tools/call", params)
    content = result["result"]["content"][0]["text"]
    
    # Should contain createBook call site in Controller AND implementation in Service
    has_controller = "com.bookstore.controller.AdminBookController" in content
    has_service = "com.bookstore.service.BookService" in content
    has_other_controller = "getBook" in content # Should still be pruned in Controller
    
    if has_controller and has_service and not has_other_controller:
        print("PASS: Depth 1 contains target and direct callee")
        return True
    else:
        print("FAIL: Depth 1 logic incorrect")
        return False

if __name__ == "__main__":
    d0 = test_depth_0()
    d1 = test_depth_1()
    if d0 and d1:
        exit(0)
    else:
        exit(1)
