/** 语义色块背景，对应 variables.css 中 --v3-tone-*，随暗黑主题自动切换 */
export const SURFACE_TONE = {
  danger: "var(--v3-tone-danger-surface-soft)",
  dangerStrong: "var(--v3-tone-danger-surface)",
  warning: "var(--v3-tone-warning-surface)",
  warningPending: "var(--v3-tone-warning-pending-surface)",
  primary: "var(--v3-tone-primary-surface)",
  success: "var(--v3-tone-success-surface)"
} as const
