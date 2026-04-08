<?php
/**
 * Dev-Pipe URL Discovery API
 *
 * Endpoints:
 *   ?action=get_url       - Get dev-pipe URL + timestamp
 *   ?action=set_url&url=  - Set dev-pipe URL
 *   ?action=get_ip        - Get cached public IP + timestamp
 *   ?action=set_ip&ip=   - Set public IP
 *   ?action=get_lan_ip    - Get server LAN (local) IP address
 *   ?action=status        - Check dev-pipe status
 *   ?action=diag          - Diagnostic info
 *   ?action=log&lines=   - Get update log (last N lines)
 *   ?action=api_token     - Get API token for app
 */

// Configuration
define('DEV_PIPE_TOKEN', 'mamarazzi-app-token-2026');
define('DEV_PIPE_API_TOKEN', 'devpipe-app-token-2026');
define('DEV_PIPE_DEFAULT_URL', 'http://localhost:8080');
define('URL_CACHE_FILE', __DIR__ . '/url_cache.txt');
define('IP_CACHE_FILE', __DIR__ . '/ip_cache.txt');
define('LOG_FILE', __DIR__ . '/update_log.txt');

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
    $data = @file_get_contents(URL_CACHE_FILE);
    if ($data !== false) {
        $decoded = json_decode($data, true);
        if (is_array($decoded) && isset($decoded['url'])) {
            return $decoded['url'];
        }
        return trim($data);
    }
    return DEV_PIPE_DEFAULT_URL;
}

function getCacheMeta(string $file): array
{
    $data = @file_get_contents($file);
    if ($data !== false) {
        $decoded = json_decode($data, true);
        if (is_array($decoded) && isset($decoded['updated'])) {
            return ['value' => $decoded['url'] ?? $decoded['ip'] ?? '', 'updated' => $decoded['updated']];
        }
        return ['value' => trim($data), 'updated' => @filemtime($file) ?: time()];
    }
    return ['value' => '', 'updated' => null];
}

function writeCacheWithMeta(string $file, string $value): bool
{
    $data = json_encode(['url' => $value, 'ip' => $value, 'updated' => date('c')]);
    return file_put_contents($file, $data) !== false;
}

function logUpdate(string $type, string $value): void
{
    $entry = date('Y-m-d H:i:s') . " [$type] $value\n";
    @file_put_contents(LOG_FILE, $entry, FILE_APPEND);
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

function getCachedIp(): string
{
    $data = @file_get_contents(IP_CACHE_FILE);
    if ($data !== false) {
        $decoded = json_decode($data, true);
        if (is_array($decoded) && isset($decoded['ip'])) {
            return $decoded['ip'];
        }
        return trim($data);
    }
    return '';
}

function isValidIp(string $ip): bool
{
    return filter_var($ip, FILTER_VALIDATE_IP) !== false;
}

/**
 * Determine the server's local LAN IP address.
 *
 * Tries hostname-based resolution first (most reliable for multi-NIC
 * servers), then falls back to the web-server-provided SERVER_ADDR.
 * Loopback addresses are skipped so the caller always receives a
 * routable LAN address when one is available.
 */
function getLocalLanIp(): string
{
    $hostname = gethostname();
    if ($hostname !== false) {
        $ip = gethostbyname($hostname);
        if (
            $ip !== $hostname
            && filter_var($ip, FILTER_VALIDATE_IP) !== false
            && $ip !== '127.0.0.1'
            && $ip !== '::1'
        ) {
            return $ip;
        }
    }

    $serverAddr = $_SERVER['SERVER_ADDR'] ?? '';
    if ($serverAddr && $serverAddr !== '127.0.0.1' && $serverAddr !== '::1') {
        return $serverAddr;
    }

    return $serverAddr ?: 'unknown';
}

// Handle actions
switch ($action) {
    case 'get_url':
        $meta = getCacheMeta(URL_CACHE_FILE);
        echo json_encode(['url' => $meta['value'] ?: DEV_PIPE_DEFAULT_URL, 'updated' => $meta['updated']]);
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
        writeCacheWithMeta(URL_CACHE_FILE, $newUrl);
        logUpdate('URL', $newUrl);
        echo json_encode(['success' => true, 'url' => $newUrl, 'updated' => date('c')]);
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

    case 'diag':
        $diag = [];
        $diag['server_ip'] = $_SERVER['SERVER_ADDR'] ?? 'unknown';
        $diag['remote_ip'] = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
        $diag['hostname'] = gethostname();
        $diag['php_version'] = PHP_VERSION;
        $diag['curl_exists'] = function_exists('curl_init');
        
        // Test: try to connect to localhost:8080
        $localTest = [];
        $localTest['target'] = 'localhost:8080';
        $start = microtime(true);
        $conn = @fsockopen('localhost', 8080, $errno, $errstr, 3);
        $localTest['connect_time_ms'] = round((microtime(true) - $start) * 1000);
        if ($conn) {
            $localTest['reachable'] = true;
            fclose($conn);
        } else {
            $localTest['reachable'] = false;
            $localTest['error'] = "$errstr ($errno)";
        }
        $diag['localhost_8080'] = $localTest;
        
        echo json_encode($diag);
        break;

    case 'get_ip':
        $meta = getCacheMeta(IP_CACHE_FILE);
        echo json_encode(['ip' => $meta['value'], 'updated' => $meta['updated']]);
        break;

    case 'get_lan_ip':
        echo json_encode(['ip' => getLocalLanIp(), 'type' => 'lan']);
        break;

    case 'log':
        $lines = isset($_GET['lines']) ? max(1, min(100, (int)$_GET['lines'])) : 20;
        $log = @file_get_contents(LOG_FILE);
        $entries = $log ? array_slice(array_filter(explode("\n", trim($log))), -$lines) : [];
        echo json_encode(['entries' => $entries]);
        break;

    case 'set_ip':
        $newIp = $_GET['ip'] ?? '';
        if (empty($newIp)) {
            http_response_code(400);
            echo json_encode(['error' => 'IP parameter required']);
            break;
        }
        if (!isValidIp($newIp)) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid IP address']);
            break;
        }
        writeCacheWithMeta(IP_CACHE_FILE, $newIp);
        logUpdate('IP', $newIp);
        echo json_encode(['success' => true, 'ip' => $newIp, 'updated' => date('c')]);
        break;

    default:
        http_response_code(400);
        echo json_encode(['error' => 'Unknown action. Use: get_url, set_url, get_ip, set_ip, get_lan_ip, status, api_token']);
}
