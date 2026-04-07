import Navbar from "../components/Navbar";
import { Box, Button, Typography } from "@mui/material";
import { useSnackbar } from "../utils/Snackbar";
import { useLoader } from "../utils/Loading";

export default function HomePage() {
  const snackbar = useSnackbar();
  const loader = useLoader();

  const handleAlert = () => {
    snackbar.success("You have successfully triggered an alert!");
  };

  const handleLoading = () => {
    loader.showLoader("Loading the template...");
  };

  return (
    <Box
      sx={{
        width: "100%",
        minHeight: "100vh",
        flexDirection: "column",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <Navbar />
      <Box
        sx={{
          width: "100%",
          minHeight: "100vh",
          flexDirection: "column",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Typography variant="h3">Welcome to the Front End template</Typography>
        <p>Lets start building on top of it!</p>
        <Box
          sx={{
            flexDirection: "row",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 2,
          }}
        >
          <Button
            sx={{ backgroundColor: "red", "&:hover": { backgroundColor: "darkred" } }}
            variant="contained"
            onClick={handleAlert}
          >
            Alert
          </Button>
          <Button
            sx={{ backgroundColor: "blue", "&:hover": { backgroundColor: "darkblue" } }}
            variant="contained"
            onClick={handleLoading}
          >
            Loading
          </Button>
        </Box>
      </Box>
    </Box>
  );
}
