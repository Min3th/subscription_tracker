import { useState } from "react";
import { Avatar, Box, Typography, Menu, MenuItem, Divider } from "@mui/material";
import { User, Settings, LogOut, ChevronDown } from "lucide-react";

interface UserProfileProps {
  user: {
    name: string;
    email: string;
    photoUrl: string;
  };
  onSignOut: () => void;
  onSettings?: () => void;
}

export default function UserProfile({ user, onSignOut, onSettings }: UserProfileProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <Box sx={{ backgroundColor: "lightpink" }}>
      <Box
        onClick={handleClick}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 1.5,
          py: 1,
          borderRadius: 2,
          cursor: "pointer",
          transition: "background-color 0.2s",
          "&:hover": {
            bgcolor: "action.hover",
          },
        }}
      >
        <Avatar src={user.photoUrl} alt={user.name} sx={{ width: 40, height: 40, border: "2px solid #e0e0e0" }} />
        <Box sx={{ display: { xs: "none", md: "block" }, textAlign: "left" }}>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {user.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {user.email}
          </Typography>
        </Box>
        <ChevronDown
          size={16}
          style={{
            color: "#757575",
            transition: "transform 0.2s",
            transform: open ? "rotate(180deg)" : "rotate(0deg)",
          }}
        />
      </Box>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
        sx={{ mt: 1 }}
        PaperProps={{
          sx: { minWidth: 220 },
        }}
      >
        {/* User info in dropdown (mobile) */}
        <Box sx={{ display: { xs: "block", md: "none" }, px: 2, py: 1.5, borderBottom: 1, borderColor: "divider" }}>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {user.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {user.email}
          </Typography>
        </Box>

        <MenuItem
          onClick={() => {
            console.log("Profile clicked");
            handleClose();
          }}
        >
          <User size={18} style={{ marginRight: 12 }} />
          Profile
        </MenuItem>

        {onSettings && (
          <MenuItem
            onClick={() => {
              onSettings();
              handleClose();
            }}
          >
            <Settings size={18} style={{ marginRight: 12 }} />
            Settings
          </MenuItem>
        )}

        <Divider />

        <MenuItem
          onClick={() => {
            onSignOut();
            handleClose();
          }}
          sx={{ color: "error.main" }}
        >
          <LogOut size={18} style={{ marginRight: 12 }} />
          Sign Out
        </MenuItem>
      </Menu>
    </Box>
  );
}
