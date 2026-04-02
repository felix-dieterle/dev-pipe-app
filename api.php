<?php
/**
 * Dev-Pipe URL Discovery API
 *
 * Place this at: public/apps/devpipe/api.php
 * 
 * Endpoints:
 *   ?token=<DEV_PIPE_TOKEN>&action=get_url      - Get dev-pipe URL
 *   ?token=<DEV_PIPE_TOKEN>&action=set_url&url=<url>  - Set dev-pipe URL
 *   ?token=<DEV_PIPE_TOKEN>&action=status         - Check dev-pipe status
 *   ?token=<DEV_PIPE_TOKEN>&action=api_token     - Get API token for app
 */

// Configuration
define('DEV_PIPE_TOKEN', 'mamarazzi-app-token-2026');
define('DEV_PIPE_API_TOKEN', 'devpipe-app-token-2026');
define('DEV_PIPE_DEFAULT_URL', 'http://localhost:8080');
define('URL_CACHE_FILE', __DIR__ . '/url_cache.txt');

header('Content-Type: application/json');

// Get parameters
$token = $_GET['token'] ?? '';
$action = $_GET['action'] ?? 'get_url';
$newUrl = $_GET['url'] ?? '';

// Validate token (timing-safe comparison)
if (!hash_equals(DEV_PIPE_TOKEN, $token)) {
    http_response_code(401);
    echo json_encode(['error' => 'Invalid token']);
    exit;
}

/**
 * Read the cached URL from disk, falling back to the default.
 */
function getCachedUrl(): string
{
    $url = @file_get_contents(URL_CACHE_FILE);
    return ($url !== false) ? trim($url) : DEV_PIPE_DEFAULT_URL;
}

/**
 * Return true when $url is an http:// or https:// URL with a non-empty host.
 */
function isValidUrl(string $url): bool
{
    $parsed = parse_url($url);
    if (!$parsed) {
        return false;
    }
    $scheme = $parsed['scheme'] ?? '';
    $host   = $parsed['host']   ?? '';
    return in_array($scheme, ['http', 'https'], true) && $host !== '';
}

// Handle actions
switch ($action) {
    case 'get_url':
        echo json_encode(['url' => getCachedUrl()]);
        break;

    case 'set_url':
        if (empty($newUrl)) {
            http_response_code(400);
            echo json_encode(['error' => 'URL parameter required']);
            break;
        }
        if (!isValidUrl($newUrl)) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid URL - must be http:// or https://']);
            break;
        }
        file_put_contents(URL_CACHE_FILE, $newUrl);
        echo json_encode(['success' => true, 'url' => $newUrl]);
        break;
        
    case 'api_token':
        echo json_encode(['token' => DEV_PIPE_API_TOKEN]);
        break;
        
    case 'status':
        $url = getCachedUrl();

        if (!function_exists('curl_init')) {
            echo json_encode(['status' => 'unknown', 'url' => $url, 'error' => 'curl not available']);
            break;
        }

        $ch = curl_init($url . '/api/health');
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Authorization: Bearer ' . DEV_PIPE_API_TOKEN]);
        curl_exec($ch);
        $curlError = curl_errno($ch);
        $httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($curlError !== CURLE_OK) {
            echo json_encode(['status' => 'offline', 'url' => $url, 'error' => curl_strerror($curlError)]);
            break;
        }

        $status = ($httpCode === 200) ? 'online' : 'offline';
        echo json_encode(['status' => $status, 'url' => $url]);
        break;

    default:
        http_response_code(400);
        echo json_encode(['error' => 'Unknown action. Use: get_url, set_url, status, api_token']);
}
