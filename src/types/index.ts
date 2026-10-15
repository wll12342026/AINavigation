export interface NavItem {
  id: string
  path: string
  label: string
  icon: string
  title: string
  subtitle: string
}

export interface ToastMessage {
  id: number
  text: string
  type: 'success' | 'warning'
}
