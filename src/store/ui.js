import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useUIStore = create(
  persist(
    (set, get) => ({
      // Theme
      theme: 'dark',
      
      // Sidebar
      sidebarCollapsed: false,
      
      // Table preferences
      tableDensity: 'comfortable', // compact, comfortable, spacious
      
      // Notifications
      notificationsEnabled: true,
      notificationSound: true,
      
      // Search
      globalSearchOpen: false,
      
      // Actions
      toggleTheme: () => {
        const { theme } = get();
        const newTheme = theme === 'dark' ? 'light' : 'dark';
        set({ theme: newTheme });
        
        // Update document class
        document.documentElement.classList.toggle('dark', newTheme === 'dark');
      },
      
      setTheme: (theme) => {
        set({ theme });
        document.documentElement.classList.toggle('dark', theme === 'dark');
      },
      
      toggleSidebar: () => {
        const { sidebarCollapsed } = get();
        set({ sidebarCollapsed: !sidebarCollapsed });
      },
      
      setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
      
      setTableDensity: (density) => set({ tableDensity: density }),
      
      toggleNotifications: () => {
        const { notificationsEnabled } = get();
        set({ notificationsEnabled: !notificationsEnabled });
      },
      
      toggleNotificationSound: () => {
        const { notificationSound } = get();
        set({ notificationSound: !notificationSound });
      },
      
      setGlobalSearchOpen: (open) => set({ globalSearchOpen: open }),
      
      // Initialize theme on mount
      initializeTheme: () => {
        const { theme } = get();
        document.documentElement.classList.toggle('dark', theme === 'dark');
      },
    }),
    {
      name: 'cyberscope-ui',
      partialize: (state) => ({
        theme: state.theme,
        sidebarCollapsed: state.sidebarCollapsed,
        tableDensity: state.tableDensity,
        notificationsEnabled: state.notificationsEnabled,
        notificationSound: state.notificationSound,
      }),
    }
  )
);

export default useUIStore;
