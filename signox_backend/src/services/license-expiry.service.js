const prisma = require('../config/db');

/**
 * Check and suspend USER_ADMIN accounts with expired licenses
 * This function should be called periodically (e.g., daily via cron job)
 */
async function checkAndSuspendExpiredLicenses() {
  try {
    console.log('🔍 Checking for expired USER_ADMIN licenses...');
    
    const now = new Date();
    
    // Find all USER_ADMIN profiles with expired licenses that are still active
    const expiredProfiles = await prisma.userAdminProfile.findMany({
      where: {
        licenseExpiry: {
          lt: now // License expiry date is less than current date
        },
        isActive: true // Only check active profiles
      },
      include: {
        userAdmin: {
          select: {
            id: true,
            email: true,
            isActive: true
          }
        }
      }
    });

    if (expiredProfiles.length === 0) {
      console.log('✅ No expired licenses found');
      return { suspended: 0, message: 'No expired licenses found' };
    }

    console.log(`⚠️  Found ${expiredProfiles.length} expired licenses`);

    let suspendedCount = 0;

    // Process each expired profile
    for (const profile of expiredProfiles) {
      if (!profile.userAdmin || !profile.userAdmin.isActive) {
        console.log(`⏭️  Skipping already inactive user: ${profile.userAdmin?.email || 'Unknown'}`);
        continue;
      }

      try {
        await prisma.$transaction(async (tx) => {
          // 1. Suspend the USER_ADMIN
          await tx.user.update({
            where: { id: profile.userAdmin.id },
            data: { isActive: false }
          });

          // 2. Suspend the UserAdminProfile
          await tx.userAdminProfile.update({
            where: { id: profile.id },
            data: { isActive: false }
          });

          // 3. Suspend all STAFF under this USER_ADMIN
          const staffUpdateResult = await tx.user.updateMany({
            where: {
              role: 'STAFF',
              createdByUserAdminId: profile.userAdmin.id,
              isActive: true // Only suspend active staff
            },
            data: { isActive: false }
          });

          // 4. Unassign displays managed by this USER_ADMIN
          await tx.display.updateMany({
            where: { 
              managedByUserId: profile.userAdmin.id 
            },
            data: {
              playlistId: null,
              layoutId: null,
              activeLayoutId: null,
              managedByUserId: null,
            }
          });

          console.log(`🔒 Suspended USER_ADMIN: ${profile.userAdmin.email} (License expired: ${profile.licenseExpiry})`);
          console.log(`   └── Also suspended ${staffUpdateResult.count} staff members`);
          
          suspendedCount++;
        });
      } catch (error) {
        console.error(`❌ Failed to suspend USER_ADMIN ${profile.userAdmin.email}:`, error);
      }
    }

    const message = `Suspended ${suspendedCount} USER_ADMIN accounts with expired licenses`;
    console.log(`✅ ${message}`);
    
    return { 
      suspended: suspendedCount, 
      total: expiredProfiles.length,
      message 
    };

  } catch (error) {
    console.error('❌ Error checking expired licenses:', error);
    throw error;
  }
}

/**
 * Get summary of license expiry status
 */
async function getLicenseExpirySummary() {
  try {
    const now = new Date();
    const thirtyDaysFromNow = new Date(now.getTime() + (30 * 24 * 60 * 60 * 1000));

    const [expired, expiringSoon, active] = await Promise.all([
      // Expired licenses
      prisma.userAdminProfile.count({
        where: {
          licenseExpiry: { lt: now },
          isActive: true
        }
      }),
      
      // Expiring within 30 days
      prisma.userAdminProfile.count({
        where: {
          licenseExpiry: {
            gte: now,
            lte: thirtyDaysFromNow
          },
          isActive: true
        }
      }),
      
      // Active licenses (not expiring soon)
      prisma.userAdminProfile.count({
        where: {
          OR: [
            { licenseExpiry: { gt: thirtyDaysFromNow } },
            { licenseExpiry: null }
          ],
          isActive: true
        }
      })
    ]);

    return {
      expired,
      expiringSoon,
      active,
      total: expired + expiringSoon + active
    };
  } catch (error) {
    console.error('❌ Error getting license summary:', error);
    throw error;
  }
}

module.exports = {
  checkAndSuspendExpiredLicenses,
  getLicenseExpirySummary
};