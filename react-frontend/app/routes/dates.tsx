import {DateCard} from "~/components/DateCard";
import {Avatar, Typography} from "@mui/material";
import logo from "./icon.png"

function Dates() {
    return (
        <>
            <div className="bg-gray-100 h-min-screen h-fit">
                <div className="flex flex-row p-4 bg-blue-800 w-full text-white items-center rounded-b-lg fixed h-24 z-50">
                    <div className="object-scale-down size-12">
                        <img src={logo} alt="logo"/>
                    </div>
                    <Typography variant="h4" color="white" className="m-2 flex-grow">Termine</Typography>
                    <Avatar
                        alt="tania andrew"
                        sx={{width: 56, height: 56}}
                        src="https://images.unsplash.com/photo-1633332755192-727a05c4013d?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1480&q=80"
                    />
                </div>
                <div className="flex flex-row flex-wrap overflow-auto pt-24">
                    <DateCard title="Gruppenstunde" start="01. Januar 2024" end="28. Februar 2025" startShort="01.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="02.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="03.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="04.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="05.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="06.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="07.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="08.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="01.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="01.01.24"
                              ort="Jugendkeller"/>
                    <DateCard title="Gruppenstunde" start="08. Januar 2024" end="28. Februar 2025" startShort="01.01.24"
                              ort="Jugendkeller"/>
                </div>
            </div>
        </>
    )
}

export default Dates
