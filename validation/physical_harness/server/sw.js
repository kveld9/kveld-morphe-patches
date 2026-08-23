// ServiceWorker for Background Sync & Periodic Sync Testing

self.addEventListener('install', event => {
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(clients.claim());
});

self.addEventListener('sync', event => {
    const timestamp = new Date().toISOString();
    const tag = event.tag;
    console.log(`[SW-SYNC] Sync event fired for tag: ${tag} at ${timestamp}`);
    
    // Broadcast execution to open clients or store in IndexedDB / BroadcastChannel
    if (self.BroadcastChannel) {
        const bc = new BroadcastChannel('sync_events');
        bc.postMessage({ type: 'SYNC_FIRED', tag: tag, timestamp: timestamp });
    }
});

self.addEventListener('periodicsync', event => {
    const timestamp = new Date().toISOString();
    const tag = event.tag;
    console.log(`[SW-PERIODIC] PeriodicSync event fired for tag: ${tag} at ${timestamp}`);
    
    if (self.BroadcastChannel) {
        const bc = new BroadcastChannel('sync_events');
        bc.postMessage({ type: 'PERIODIC_SYNC_FIRED', tag: tag, timestamp: timestamp });
    }
});
