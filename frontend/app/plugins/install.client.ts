interface InstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed', platform: string }>
}

export default defineNuxtPlugin(() => {
  const canInstall = ref(false)
  let deferredPrompt: InstallPromptEvent | undefined

  window.addEventListener('beforeinstallprompt', event => {
    event.preventDefault()
    deferredPrompt = event as InstallPromptEvent
    canInstall.value = true
  })

  window.addEventListener('appinstalled', () => {
    deferredPrompt = undefined
    canInstall.value = false
  })

  return {
    provide: {
      canInstallAdmin: canInstall,
      installAdmin: async () => {
        if (!deferredPrompt) return false
        await deferredPrompt.prompt()
        const result = await deferredPrompt.userChoice
        deferredPrompt = undefined
        canInstall.value = false
        return result.outcome === 'accepted'
      }
    }
  }
})
