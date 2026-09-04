const covers: Record<string, string> = {
  'ningbo-citizen-card-rights-confirmation': '/covers/ningbo-citizen-card.jpg',
  'distributed-certificate-storage': '/covers/distributed-certificate.jpg',
  'personal-content-ai-site': '/covers/personal-site.jpg'
}

export function useProjectCovers() {
  const coverFor = (slug: string) => covers[slug] || null
  return { coverFor }
}
