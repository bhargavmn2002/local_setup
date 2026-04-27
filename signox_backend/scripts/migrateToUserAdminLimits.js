const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

/**
 * Migration script to move limits from CLIENT_ADMIN level to USER_ADMIN level
 * 
 * This script:
 * 1. Creates UserAdminProfile for existing USER_ADMIN users
 * 2. Copies limits from their parent CLIENT_ADMIN's ClientProfile
 * 3. Removes limit fields from ClientProfile (handled by schema change)
 * 
 * Run this after updating the schema but before deploying the new code.
 */
async function main() {
  console.log('🚀 Starting migration: Moving limits from CLIENT_ADMIN to USER_ADMIN level...');

  try {
    // Find all USER_ADMIN users that don't have a UserAdminProfile yet
    const userAdmins = await prisma.user.findMany({
      where: {
        role: 'USER_ADMIN',
        userAdminProfile: null, // Only migrate users without profile
      },
      include: {
        managedByClientAdmin: {
          include: {
            clientProfile: true,
          },
        },
      },
    });

    console.log(`📊 Found ${userAdmins.length} USER_ADMIN users to migrate`);

    if (userAdmins.length === 0) {
      console.log('✅ No USER_ADMIN users need migration. All done!');
      return;
    }

    let migratedCount = 0;
    let errorCount = 0;

    for (const userAdmin of userAdmins) {
      try {
        const clientProfile = userAdmin.managedByClientAdmin?.clientProfile;
        
        if (!clientProfile) {
          console.warn(`⚠️  USER_ADMIN ${userAdmin.email} has no parent CLIENT_ADMIN profile. Skipping.`);
          continue;
        }

        // Create UserAdminProfile with limits from parent CLIENT_ADMIN
        await prisma.userAdminProfile.create({
          data: {
            userAdminId: userAdmin.id,
            maxDisplays: clientProfile.maxDisplays || 10,
            maxUsers: clientProfile.maxUsers || 5,
            maxStorageMB: clientProfile.maxStorageMB || 25,
            maxMonthlyUsageMB: clientProfile.maxMonthlyUsageMB || 150,
            monthlyUploadedBytes: clientProfile.monthlyUploadedBytes || BigInt(0),
            usageQuotaResetDate: clientProfile.usageQuotaResetDate || new Date(),
            billingDayOfMonth: clientProfile.billingDayOfMonth || 1,
            licenseExpiry: clientProfile.licenseExpiry,
            isActive: clientProfile.isActive,
          },
        });

        console.log(`✅ Migrated USER_ADMIN: ${userAdmin.email}`);
        migratedCount++;
      } catch (error) {
        console.error(`❌ Error migrating USER_ADMIN ${userAdmin.email}:`, error.message);
        errorCount++;
      }
    }

    console.log('\n📈 Migration Summary:');
    console.log(`✅ Successfully migrated: ${migratedCount} USER_ADMIN users`);
    console.log(`❌ Errors: ${errorCount} USER_ADMIN users`);
    
    if (errorCount === 0) {
      console.log('\n🎉 Migration completed successfully!');
      console.log('\n📝 Next steps:');
      console.log('1. Deploy the updated backend code');
      console.log('2. Update frontend to use userAdminProfile instead of clientProfile for limits');
      console.log('3. Test user creation and limit enforcement');
    } else {
      console.log('\n⚠️  Migration completed with errors. Please review and fix the failed migrations.');
    }

  } catch (error) {
    console.error('💥 Migration failed:', error);
    process.exit(1);
  }
}

main()
  .catch((e) => {
    console.error('💥 Migration script error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });