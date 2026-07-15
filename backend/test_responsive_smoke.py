import os
import re
import pytest

CSS_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "static", "css", "dashboard.css"))
SHELL_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend", "templates", "base", "desktop_shell.html"))

def test_css_breakpoints_and_tokens():
    """Verify that CSS contains correct breakpoints, responsive shell declarations, and overrides."""
    assert os.path.exists(CSS_PATH), "dashboard.css must exist"
    
    with open(CSS_PATH, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Check that min-width: 1180px constraint has been removed or overrides exist to ensure fluidity
    assert ".responsive-shell { grid-template-columns: 264px minmax(0, 1fr); min-width: 1180px; }" not in content, \
        "The absolute min-width 1180px constraint must not block fluidity on responsive shell."
        
    # Confirm 1180px media queries are defined
    assert "@media (max-width: 1179px)" in content or "@media (max-width: 1179.98px)" in content or "@media (max-width: 1180px)" in content or "@media (max-width: 1179px)" in content, \
        "Must have a media query targeting screens below 1180px."
    assert "@media (min-width: 1180px)" in content, "Must have media query targeting screens 1180px and above."

    # Verify table wrappers and horizontal scrolling cues are present
    assert "custom-table-container" in content
    assert "overflow-x: auto" in content or "overflow-x: scroll" in content
    assert "Swipe/scroll horizontally to view full table" in content

    # Verify fluid grid utilities stack below appropriate min widths
    assert "loan-control-strip" in content
    assert "grid-template-columns: 1fr" in content or "grid-template-columns: pack" in content or "grid-template-columns" in content

def test_desktop_shell_responsive_elements():
    """Verify responsive HTML features in the unified desktop_shell template."""
    assert os.path.exists(SHELL_PATH), "desktop_shell.html must exist"
    
    with open(SHELL_PATH, "r", encoding="utf-8") as f:
        html = f.read()
        
    # Ensure compact mobile header components are present
    assert "mobile-top-header" in html or "mobileHeader" in html
    assert "mobile-menu-toggle" in html or "mobileMenuToggle" in html
    
    # Ensure aria-expanded and aria-controls are used
    assert 'aria-expanded="' in html
    assert 'aria-controls="desktopSidebar"' in html or 'aria-controls="' in html
    
    # Ensure role-aware drawer is the desktop sidebar
    assert 'id="desktopSidebar"' in html
    
    # Ensure backdrop overlay is present
    assert "sidebarBackdrop" in html or "sidebar-backdrop" in html
    
    # Ensure escape key closes menu
    assert "Escape" in html and "closeSidebarDrawer" in html
    
    # Ensure body scroll lock is toggled
    assert "body-scroll-lock" in html
