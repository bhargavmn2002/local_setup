const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const prisma = new PrismaClient();

// In-memory store for login attempts (in production, use Redis)
const loginAttempts = new Map();

const MAX_LOGIN_ATTEMPTS = parseInt(process.env.MAX_LOGIN_ATTEMPTS) || 5;
const LOCKOUT_TIME = parseInt(process.env.LOCKOUT_TIME) || 15 * 60 * 1000; // 15 minutes

// Helper function to check if IP is locked out
const isLockedOut = async (ip) => {
  const attempts = loginAttempts.get(ip);
  if (!attempts) return false;

  if (attempts.count >= MAX_LOGIN_ATTEMPTS) {
    console.warn(`High login attempts from IP: ${ip}`);
    
    // Add small delay instead of block
    await new Promise(resolve => setTimeout(resolve, 2000));
  }

  return false;
};

// Helper function to record failed login attempt
const recordFailedAttempt = (ip) => {
  const attempts = loginAttempts.get(ip) || { count: 0, lastAttempt: 0 };
  attempts.count += 1;
  attempts.lastAttempt = Date.now();
  loginAttempts.set(ip, attempts);
};

// Helper function to clear login attempts on successful login
const clearLoginAttempts = (ip) => {
  loginAttempts.delete(ip);
};

exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;
    const clientIP = req.ip || req.connection.remoteAddress;

    // Input validation
    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password required' });
    }

    // Email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: 'Invalid email format' });
    }

    // Check if IP is locked out
    if (await isLockedOut(clientIP)) {
      console.log(`Login attempt blocked: IP locked out - ${clientIP}`);
      return res.status(429).json({ 
        message: 'Too many failed login attempts. Please try again later.',
        lockoutTime: LOCKOUT_TIME / 1000 / 60 // minutes
      });
    }

    if (!process.env.JWT_SECRET) {
      console.error('JWT_SECRET is not set in environment variables');
      return res.status(500).json({ message: 'Server configuration error' });
    }

    const user = await prisma.user.findUnique({
      where: { email: email.toLowerCase().trim() },
     include: {
        clientProfile: true,
        userAdminProfile: true,
        managedByClientAdmin: {
          include: {
            clientProfile: true,
          },
        },
        createdByUserAdmin: {
          include: {
            userAdminProfile: true,
            managedByClientAdmin: {
              include: {
                clientProfile: true,
              },
            },
          },
        },
      },
    });

    if (!user) {
      console.log(`Login attempt failed: User not found - ${email}`);
      recordFailedAttempt(clientIP);
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Check if user account is active
    if (!user.isActive) {
      console.log(`Login attempt failed: User account suspended - ${email}`);
      return res.status(401).json({ message: 'Account is suspended' });
    }

    // Check if parent Client Admin is active (for User Admins)
    if (user.role === 'USER_ADMIN' && user.managedByClientAdmin) {
      if (!user.managedByClientAdmin.isActive) {
        console.log(`Login attempt failed: Parent Client Admin suspended - ${email}`);
        return res.status(401).json({ message: 'Your organization account is suspended. Please contact support.' });
      }
      
      // Check if Client Admin's profile is active
      if (user.managedByClientAdmin.clientProfile) {
        if (!user.managedByClientAdmin.clientProfile.isActive) {
          console.log(`Login attempt failed: Client profile suspended - ${email}`);
          return res.status(401).json({ message: 'Your organization license is suspended. Please contact support.' });
        }
        
        // Note: License expiry is now managed at USER_ADMIN level, not CLIENT_ADMIN level
      }

      // Check USER_ADMIN's own profile and license
      if (user.userAdminProfile) {
        if (!user.userAdminProfile.isActive) {
          console.log(`Login attempt failed: User Admin profile suspended - ${email}`);
          return res.status(401).json({ message: 'Your account is suspended. Please contact support.' });
        }
        
        // Check USER_ADMIN license expiry (this is where licenses are managed now)
        if (user.userAdminProfile.licenseExpiry && 
            new Date(user.userAdminProfile.licenseExpiry) < new Date()) {
          console.log(`Login attempt failed: User Admin license expired - ${email}`, { 
            licenseExpiry: user.userAdminProfile.licenseExpiry 
          });
          return res.status(401).json({ message: 'Your license has expired. Please contact your administrator to renew.' });
        }
      }
    }

    // Check if parent User Admin and Client Admin are active (for Staff)
    if (user.role === 'STAFF' && user.createdByUserAdmin) {
      // Check User Admin status
      if (!user.createdByUserAdmin.isActive) {
        console.log(`Login attempt failed: Parent User Admin suspended - ${email}`);
        return res.status(401).json({ message: 'Your manager account is suspended. Please contact support.' });
      }

      // Check User Admin's profile and license
      if (user.createdByUserAdmin.userAdminProfile) {
        if (!user.createdByUserAdmin.userAdminProfile.isActive) {
          console.log(`Login attempt failed: User Admin profile suspended (via Staff) - ${email}`);
          return res.status(401).json({ message: 'Your manager account is suspended. Please contact support.' });
        }
        
        // Check User Admin license expiry
        if (user.createdByUserAdmin.userAdminProfile.licenseExpiry && 
            new Date(user.createdByUserAdmin.userAdminProfile.licenseExpiry) < new Date()) {
          console.log(`Login attempt failed: User Admin license expired (via Staff) - ${email}`, { 
            licenseExpiry: user.createdByUserAdmin.userAdminProfile.licenseExpiry 
          });
          return res.status(401).json({ message: 'Your manager license has expired. Please contact your administrator.' });
        }
      }

      // Check Client Admin status
      if (user.createdByUserAdmin.managedByClientAdmin) {
        if (!user.createdByUserAdmin.managedByClientAdmin.isActive) {
          console.log(`Login attempt failed: Parent Client Admin suspended (via Staff) - ${email}`);
          return res.status(401).json({ message: 'Your organization account is suspended. Please contact support.' });
        }

        // Check Client Admin's profile
        if (user.createdByUserAdmin.managedByClientAdmin.clientProfile) {
          if (!user.createdByUserAdmin.managedByClientAdmin.clientProfile.isActive) {
            console.log(`Login attempt failed: Client profile suspended (via Staff) - ${email}`);
            return res.status(401).json({ message: 'Your organization license is suspended. Please contact support.' });
          }
          
          // Note: License expiry is now managed at USER_ADMIN level, not CLIENT_ADMIN level
        }

        // Check USER_ADMIN license expiry (STAFF inherits from their USER_ADMIN)
        if (user.createdByUserAdmin.userAdminProfile) {
          if (!user.createdByUserAdmin.userAdminProfile.isActive) {
            console.log(`Login attempt failed: User Admin profile suspended (via Staff) - ${email}`);
            return res.status(401).json({ message: 'Your User Admin account is suspended. Please contact your administrator.' });
          }

          if (user.createdByUserAdmin.userAdminProfile.licenseExpiry && 
              new Date(user.createdByUserAdmin.userAdminProfile.licenseExpiry) < new Date()) {
            console.log(`Login attempt failed: User Admin license expired (via Staff) - ${email}`, { 
              licenseExpiry: user.createdByUserAdmin.userAdminProfile.licenseExpiry 
            });
            return res.status(401).json({ message: 'Your User Admin license has expired. Please contact your administrator.' });
          }
        }
      }
    }

    // Check if Client Admin's own profile is active
    if (user.role === 'CLIENT_ADMIN' && user.clientProfile) {
      if (!user.clientProfile.isActive) {
        console.log(`Login attempt failed: Client profile suspended - ${email}`);
        return res.status(401).json({ message: 'Your organization license is suspended. Please contact support.' });
      }
      
      // Note: License expiry is now managed at USER_ADMIN level, not CLIENT_ADMIN level
      // CLIENT_ADMIN doesn't have individual license expiry anymore
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      console.log(`Login attempt failed: Invalid password - ${email}`);
      recordFailedAttempt(clientIP);
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    // Clear failed attempts on successful login
    clearLoginAttempts(clientIP);

    // Generate JWT token
    const token = jwt.sign(
      { 
        id: user.id, 
        role: user.role,
        iat: Math.floor(Date.now() / 1000)
      },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
    );

    // Fetch full user data with relations
    const userWithProfile = await prisma.user.findUnique({
      where: { id: user.id },
      include: {
        clientProfile: true,
        userAdminProfile: true,
      },
    });

    console.log(`Login successful: ${email} from IP: ${clientIP}`);
    
    // Don't send sensitive data
    const { password: _, ...userResponse } = userWithProfile;
    
    // Convert BigInt to number for JSON serialization
    const clientProfileForResponse = userResponse.clientProfile ? {
      ...userResponse.clientProfile,
      // monthlyUploadedBytes removed - now in userAdminProfile
    } : null;

    const userAdminProfileForResponse = userResponse.userAdminProfile ? {
      ...userResponse.userAdminProfile,
      monthlyUploadedBytes: Number(userResponse.userAdminProfile.monthlyUploadedBytes || 0)
    } : null;
    
    return res.json({
      accessToken: token,
      user: {
        id: userResponse.id,
        email: userResponse.email,
        role: userResponse.role,
        staffRole: userResponse.staffRole,
        isActive: userResponse.isActive,
        clientProfile: clientProfileForResponse,
        userAdminProfile: userAdminProfileForResponse,
      },
    });
  } catch (error) {
    console.error('Login error:', error);
    const code = error.code;
    const msg = error.message || '';
    const isDbUnreachable =
      code === 'P1001' ||
      code === 'P2010' ||
      /Server selection timeout|connection refused|Can't reach database|ECONNREFUSED/i.test(msg);
    if (isDbUnreachable) {
      return res.status(503).json({
        message:
          'Cannot reach the database. Run MongoDB locally or set DATABASE_URL in .env to your MongoDB connection string (e.g. Atlas).',
      });
    }
    return res.status(500).json({ message: 'Server error' });
  }
};
exports.me = async (req, res) => {
  try {
    // Fetch full user data from database
    const user = await prisma.user.findUnique({
      where: { id: req.user.id },
      include: {
        clientProfile: true,
        userAdminProfile: true,
      },
    });

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Convert BigInt to number for JSON serialization
    const clientProfileForResponse = user.clientProfile ? {
      ...user.clientProfile,
      // monthlyUploadedBytes removed - now in userAdminProfile
    } : null;

    const userAdminProfileForResponse = user.userAdminProfile ? {
      ...user.userAdminProfile,
      monthlyUploadedBytes: Number(user.userAdminProfile.monthlyUploadedBytes || 0)
    } : null;

    res.json({
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        staffRole: user.staffRole,
        isActive: user.isActive,
        clientProfile: clientProfileForResponse,
        userAdminProfile: userAdminProfileForResponse,
      },
    });
  } catch (error) {
    console.error('Me endpoint error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};
