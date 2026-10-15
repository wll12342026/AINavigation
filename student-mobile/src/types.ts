export type TabKey = 'home' | 'campus' | 'assistant' | 'tasks' | 'profile'

export type ScreenProps = {
  goTo: (tab: TabKey) => void
  notify: (message: string) => void
}
