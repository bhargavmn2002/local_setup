const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient();

async function fixScrollTextSections() {
  try {
    console.log('🔍 Finding sections with textConfig but wrong sectionType...');
    
    // Find all sections that have textConfig but sectionType is not SCROLL_TEXT
    const sectionsToFix = await prisma.layoutSection.findMany({
      where: {
        textConfig: {
          not: null
        },
        sectionType: {
          not: 'SCROLL_TEXT'
        }
      },
      select: {
        id: true,
        name: true,
        sectionType: true,
        textConfig: true,
        layout: {
          select: {
            id: true,
            name: true
          }
        }
      }
    });

    console.log(`📊 Found ${sectionsToFix.length} sections to fix`);

    if (sectionsToFix.length === 0) {
      console.log('✅ No sections need fixing');
      return;
    }

    // Show what will be fixed
    sectionsToFix.forEach(section => {
      console.log(`  - Layout: ${section.layout.name}, Section: ${section.name} (${section.sectionType} -> SCROLL_TEXT)`);
    });

    // Update all sections
    const result = await prisma.layoutSection.updateMany({
      where: {
        id: {
          in: sectionsToFix.map(s => s.id)
        }
      },
      data: {
        sectionType: 'SCROLL_TEXT'
      }
    });

    console.log(`✅ Fixed ${result.count} sections`);
    console.log('🎉 All scroll text sections now have correct sectionType!');

  } catch (error) {
    console.error('❌ Error fixing scroll text sections:', error);
  } finally {
    await prisma.$disconnect();
  }
}

// Run the fix
fixScrollTextSections();