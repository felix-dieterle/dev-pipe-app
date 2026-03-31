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
        // Return cached URL or default
        echo json_encode(['url' => getCachedUrl()]);
        break;

    case 'set_url':
        // Update cached URL (admin only)
        if (empty($newUrl)) {
            http_response_code(400);
            echo json_encode(['error' => 'URL parameter required']);
            break;
        }
        if (!isValidUrl($newUrl)) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid URL – must be http:// or https://']);
            break;
        }
        file_put_contents(URL_CACHE_FILE, $newUrl);
        echo json_encode(['success' => true, 'url' => $newUrl]);
        break;

    case 'status':
        // Check if dev-pipe is reachable
        $url = getCachedUrl();

        if (!function_exists('curl_init')) {
            echo json_encode(['status' => 'unknown', 'url' => $url, 'error' => 'curl not available']);
            break;
        }

        $ch = curl_init($url . '/api/health');
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
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
        echo json_encode(['error' => 'Unknown action']);
}
