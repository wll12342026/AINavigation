import type { PropsWithChildren, ReactNode } from 'react'
import { StyleSheet, Text, TouchableOpacity, View, type ViewStyle } from 'react-native'
import { Ionicons } from '@expo/vector-icons'
import { colors, shadow } from '../theme'

export function Card({ children, style }: PropsWithChildren<{ style?: ViewStyle | ViewStyle[] }>) {
  return <View style={[styles.card, style]}>{children}</View>
}

export function SectionTitle({ title, action, onPress }: { title: string; action?: string; onPress?: () => void }) {
  return (
    <View style={styles.sectionTitle}>
      <Text style={styles.sectionHeading}>{title}</Text>
      {action ? (
        <TouchableOpacity onPress={onPress} activeOpacity={0.7}>
          <Text style={styles.sectionAction}>{action}</Text>
        </TouchableOpacity>
      ) : null}
    </View>
  )
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <View style={styles.pageHeader}>
      <View style={{ flex: 1 }}>
        <Text style={styles.pageTitle}>{title}</Text>
        {subtitle ? <Text style={styles.pageSubtitle}>{subtitle}</Text> : null}
      </View>
      {action}
    </View>
  )
}

export function Badge({ children, tone = 'blue' }: PropsWithChildren<{ tone?: 'blue' | 'green' | 'orange' | 'violet' }>) {
  const palette = {
    blue: [colors.primarySoft, colors.primary],
    green: [colors.successSoft, colors.teal],
    orange: [colors.warningSoft, colors.orange],
    violet: ['#F0EDFF', colors.violet],
  }[tone]
  return (
    <View style={[styles.badge, { backgroundColor: palette[0] }]}>
      <Text style={[styles.badgeText, { color: palette[1] }]}>{children}</Text>
    </View>
  )
}

export function PrimaryButton({ label, icon = 'navigate', onPress }: { label: string; icon?: keyof typeof Ionicons.glyphMap; onPress?: () => void }) {
  return (
    <TouchableOpacity style={styles.primaryButton} onPress={onPress} activeOpacity={0.86}>
      <Ionicons name={icon} color="white" size={18} />
      <Text style={styles.primaryButtonText}>{label}</Text>
    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.white,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#EDF1F7',
    ...shadow,
  },
  sectionTitle: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  sectionHeading: { color: colors.ink, fontSize: 18, fontWeight: '800' },
  sectionAction: { color: colors.primary, fontSize: 13, fontWeight: '700' },
  pageHeader: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 16, flexDirection: 'row', alignItems: 'center' },
  pageTitle: { color: colors.ink, fontSize: 26, fontWeight: '900' },
  pageSubtitle: { color: colors.muted, fontSize: 12, marginTop: 5 },
  badge: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 999 },
  badgeText: { fontSize: 11, fontWeight: '800' },
  primaryButton: {
    height: 52,
    borderRadius: 16,
    backgroundColor: colors.primary,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  primaryButtonText: { color: colors.white, fontWeight: '800', fontSize: 15 },
})
