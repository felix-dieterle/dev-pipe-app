<?php
/**
 * Dev-Pipe URL Discovery API
 * 
 * Place this at: public/apps/devpipe/api.php
 * 
 * Configuration:
 *   Set DEV_PIPE_TOKEN to a secure random string
 *   Set DEV_PIPE_DEFAULT_URL as fallback
 */

// Configuration - UPDATE THESE
define('DEV_PIPE_TOKEN', 'your-secure-token-here');
define('DEV_PIPE_DEFAULT_URL', 'http://localhost:8080');
define('URL_CACHE_FILE', __DIR__ . '/url_cache.txt');

// Get parameters
$token = $_GET['token'] ?? '';
$action = $_GET['action'] ?? 'get_url';
$newUrl = $_GET['url'] ?? '';

// Validate token
if ($token !== DEV_PIPE_TOKEN) {
    http_response_code(401);
    echo json_encode(['error' => 'Invalid token']);
    exit;
}

// Handle actions
switch ($action) {
    case 'get_url':
        // Return cached URL or default
        $url = @file_get_contents(URL_CACHE_FILE);
        if (!$url) {
            $url = DEV_PIPE_DEFAULT_URL;
        }
        echo json_encode(['url' => $url]);
        break;
        
    case 'set_url':
        // Update cached URL (admin only)
        if (!empty($newUrl)) {
            file_put_contents(URL_CACHE_FILE, $newUrl);
            echo json_encode(['success' => true, 'url' => $newUrl]);
        } else {
            http_response_code(400);
            echo json_encode(['error' => 'URL parameter required']);
        }
        break;
        
    case 'status':
        // Check if dev-pipe is reachable
        $url = @file_get_contents(URL_CACHE_FILE);
        if (!$url) {
            $url = DEV_PIPE_DEFAULT_URL;
        }
        
        $ch = curl_init($url . '/api/health');
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode === 200) {
            echo json_encode(['status' => 'online', 'url' => $url]);
        } else {
            echo json_encode(['status' => 'offline', 'url' => $url]);
        }
        break;
        
    default:
        http_response_code(400);
        echo json_encode(['error' => 'Unknown action']);
}
