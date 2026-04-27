const prisma = require('../config/db');

/**
 * Calculate display status based on last heartbeat
 * @param {Object} display - Display object with lastHeartbeat and isPaired
 * @returns {string} Status: 'ONLINE', 'OFFLINE', 'PAIRING', 'ERROR'
 */
const calculateDisplayStatus = (display) => {
  if (!display.isPaired) {
    return 'PAIRING';
  }
  
  if (!display.lastHeartbeat) {
    return 'OFFLINE';
  }
  
  const now = new Date();
  const lastHeartbeat = new Date(display.lastHeartbeat);
  const timeDiff = (now - lastHeartbeat) / 1000; // seconds
  
  // Consider display offline if no heartbeat for 60 seconds or more
  // (heartbeat interval is 30 seconds, so 60 seconds gives some buffer)
  if (timeDiff >= 60) {
    return 'OFFLINE';
  }
  
  return 'ONLINE';
};

/**
 * Update display statuses in the database based on real-time heartbeat data
 * This service runs periodically to keep the database status field in sync
 */
const updateDisplayStatuses = async () => {
  try {
    console.log('🔄 [DISPLAY STATUS] Starting display status update...');
    
    // Get all paired displays
    const displays = await prisma.display.findMany({
      where: { isPaired: true },
      select: {
        id: true,
        lastHeartbeat: true,
        isPaired: true,
        status: true
      }
    });

    let updatedCount = 0;
    const batchUpdates = [];

    for (const display of displays) {
      const calculatedStatus = calculateDisplayStatus(display);
      
      // Only update if status has changed
      if (display.status !== calculatedStatus) {
        batchUpdates.push({
          id: display.id,
          newStatus: calculatedStatus
        });
      }
    }

    // Perform batch updates
    for (const update of batchUpdates) {
      await prisma.display.update({
        where: { id: update.id },
        data: { status: update.newStatus }
      });
      updatedCount++;
    }

    console.log(`✅ [DISPLAY STATUS] Updated ${updatedCount} display statuses out of ${displays.length} total displays`);
    
    return {
      success: true,
      totalDisplays: displays.length,
      updatedCount,
      timestamp: new Date()
    };
  } catch (error) {
    console.error('❌ [DISPLAY STATUS] Error updating display statuses:', error);
    return {
      success: false,
      error: error.message,
      timestamp: new Date()
    };
  }
};

/**
 * Start the display status update service
 * Runs every 30 seconds to keep database status in sync
 */
const startDisplayStatusService = () => {
  console.log('🚀 [DISPLAY STATUS] Starting display status update service...');
  
  // Run immediately on startup
  updateDisplayStatuses();
  
  // Then run every 30 seconds
  const interval = setInterval(updateDisplayStatuses, 30000);
  
  // Return the interval so it can be cleared if needed
  return interval;
};

/**
 * Stop the display status update service
 */
const stopDisplayStatusService = (interval) => {
  if (interval) {
    clearInterval(interval);
    console.log('🛑 [DISPLAY STATUS] Display status update service stopped');
  }
};

module.exports = {
  calculateDisplayStatus,
  updateDisplayStatuses,
  startDisplayStatusService,
  stopDisplayStatusService
};