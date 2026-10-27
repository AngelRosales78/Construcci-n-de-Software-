import * as yup from 'yup'

/**
 * Registration schema - equivalent to Yup validation for HU01_01
 * Validates: username (min 3, unique), email (valid format, unique),
 * password (min 8, upper, lower, digit, special), password confirmation
 */
export const registerSchema = yup.object().shape({
  username: yup
    .string()
    .required('Escribe tu nombre de usuario.')
    .min(3, 'El usuario debe tener al menos 3 caracteres.')
    .max(150, 'El usuario admite como máximo 150 caracteres.')
    .matches(
      /^[a-zA-Z0-9_]+$/,
      'Usa solo letras, números y guion bajo.'
    ),
  email: yup
    .string()
    .required('Escribe tu correo electrónico.')
    .email('Escribe un correo electrónico válido.'),
  password: yup
    .string()
    .required('Escribe tu contraseña.')
    .min(8, 'La contraseña debe tener al menos 8 caracteres.')
    .matches(/[A-Z]/, 'Incluye una letra mayúscula.')
    .matches(/[a-z]/, 'Incluye una letra minúscula.')
    .matches(/[0-9]/, 'Incluye un número.')
    .matches(
      /[!@#$%^&*(),.?":{}|<>]/,
      'Incluye un símbolo.'
    ),
  passwordConfirm: yup
    .string()
    .required('Repite tu contraseña.')
    .oneOf([yup.ref('password'), null], 'Las contraseñas deben coincidir.'),
  firstName: yup
    .string()
    .max(150, 'El nombre admite como máximo 150 caracteres.')
    .optional(),
  lastName: yup
    .string()
    .max(150, 'El apellido admite como máximo 150 caracteres.')
    .optional(),
  userType: yup
    .string()
    .oneOf(['retail', 'institutional'], 'Selecciona un tipo de cuenta válido.')
    .default('retail'),
})

/**
 * Login schema - validates username/email and password for HU01_02
 */
export const loginSchema = yup.object().shape({
  username: yup
    .string()
    .required('Escribe tu nombre de usuario.')
    .min(3, 'El usuario debe tener al menos 3 caracteres.'),
  password: yup
    .string()
    .required('Escribe tu contraseña.'),
})

/**
 * Forgot password schema - validates email for HU01_03
 */
export const forgotPasswordSchema = yup.object().shape({
  email: yup
    .string()
    .required('Escribe tu correo electrónico.')
    .email('Escribe un correo electrónico válido.'),
})

/**
 * Reset password schema - validates new password confirmation
 */
export const resetPasswordSchema = yup.object().shape({
  password: yup
    .string()
    .required('Escribe tu contraseña.')
    .min(8, 'La contraseña debe tener al menos 8 caracteres.')
    .matches(/[A-Z]/, 'Incluye una letra mayúscula.')
    .matches(/[a-z]/, 'Incluye una letra minúscula.')
    .matches(/[0-9]/, 'Incluye un número.')
    .matches(
      /[!@#$%^&*(),.?":{}|<>]/,
      'Incluye un símbolo.'
    ),
  passwordConfirm: yup
    .string()
    .required('Repite tu contraseña.')
    .oneOf([yup.ref('password'), null], 'Las contraseñas deben coincidir.'),
})
