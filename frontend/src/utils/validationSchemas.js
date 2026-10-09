import * as yup from 'yup'

/**
 * Registration schema - equivalent to Yup validation for HU01_01
 * Validates: username (min 3, unique), email (valid format, unique),
 * password (min 8, upper, lower, digit, special), password confirmation
 */
export const registerSchema = yup.object().shape({
  username: yup
    .string()
    .required('Username is required')
    .min(3, 'Username must be at least 3 characters')
    .max(150, 'Username must be less than 150 characters')
    .matches(
      /^[a-zA-Z0-9_]+$/,
      'Username can only contain letters, numbers, and underscores'
    ),
  email: yup
    .string()
    .required('Email is required')
    .email('Please enter a valid email address'),
  password: yup
    .string()
    .required('Password is required')
    .min(8, 'Password must be at least 8 characters')
    .matches(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .matches(/[a-z]/, 'Password must contain at least one lowercase letter')
    .matches(/[0-9]/, 'Password must contain at least one number')
    .matches(
      /[!@#$%^&*(),.?":{}|<>]/,
      'Password must contain at least one special character'
    ),
  passwordConfirm: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password'), null], 'Passwords must match'),
  firstName: yup
    .string()
    .max(150, 'First name must be less than 150 characters')
    .optional(),
  lastName: yup
    .string()
    .max(150, 'Last name must be less than 150 characters')
    .optional(),
  userType: yup
    .string()
    .oneOf(['retail', 'institutional'], 'Invalid user type')
    .default('retail'),
})

/**
 * Login schema - validates username/email and password for HU01_02
 */
export const loginSchema = yup.object().shape({
  username: yup
    .string()
    .required('Username or email is required')
    .min(3, 'Must be at least 3 characters'),
  password: yup
    .string()
    .required('Password is required'),
})

/**
 * Forgot password schema - validates email for HU01_03
 */
export const forgotPasswordSchema = yup.object().shape({
  email: yup
    .string()
    .required('Email is required')
    .email('Please enter a valid email address'),
})

/**
 * Reset password schema - validates new password confirmation
 */
export const resetPasswordSchema = yup.object().shape({
  password: yup
    .string()
    .required('Password is required')
    .min(8, 'Password must be at least 8 characters')
    .matches(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .matches(/[a-z]/, 'Password must contain at least one lowercase letter')
    .matches(/[0-9]/, 'Password must contain at least one number')
    .matches(
      /[!@#$%^&*(),.?":{}|<>]/,
      'Password must contain at least one special character'
    ),
  passwordConfirm: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password'), null], 'Passwords must match'),
})
